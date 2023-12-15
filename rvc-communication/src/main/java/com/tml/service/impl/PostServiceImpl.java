package com.tml.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tml.feign.user.RvcUserServiceFeignClient;
import com.tml.mapper.*;
import com.tml.mq.handler.ProducerHandler;
import com.tml.pojo.dto.CoinDto;
import com.tml.pojo.dto.DetectionTaskDto;
import com.tml.pojo.dto.PageInfo;
import com.tml.pojo.dto.PostDto;
import com.tml.pojo.entity.*;
import com.tml.pojo.vo.PostVo;
import com.tml.service.PostService;
import com.tml.strategy.SortStrategy;
import com.tml.strategy.impl.LikeSortStrategy;
import com.tml.strategy.impl.TimeSortStrategy;
import com.tml.strategy.impl.ViewSortStrategy;
import com.tml.utils.BeanCopyUtils;
import com.tml.utils.BeanUtils;
import com.tml.utils.RedisCache;
import com.tml.utils.Uuid;
import io.github.common.web.Result;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import static com.tml.constant.DBConstant.RVC_COMMUNICATION_POST_WATCH;
import static com.tml.constant.DetectionConstants.DETECTION_SUCCESS;
import static com.tml.constant.DetectionConstants.UN_DETECTION;

/**
 * @NAME: PostServiceImpl
 * @USER: yuech
 * @Description:
 * @DATE: 2023/12/5
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final CollectPostMapper collectPostMapper;
    private final LikePostMapper likePostMapper;
    private final CoverMapper coverMapper;
    private final PostMapper postMapper;
    private final ThreadPoolTaskExecutor executor;
    private final RvcUserServiceFeignClient rvcUserServiceFeignClient;
    private final RedisCache redisCache;
    private final PostTypeMapper postTypeMapper;
//    private final RedisCache redisCache;

    private final Map<String, SortStrategy> strategyMap = new HashMap<>();
    {
        strategyMap.put("1", new TimeSortStrategy());
        strategyMap.put("2", new LikeSortStrategy());
        strategyMap.put("3", new ViewSortStrategy());
    }


    @Override
    public List<PostVo> list(PageInfo<String> params,String tagId) {

        /**
         * 模拟获取uuid
         */
        String uuid = "1735241323452608514";

        //分页
        Integer pageNum = params.getPage();
        Integer pageSize = params.getLimit();
        Page<Post> page = new Page<>(pageNum,pageSize);
        LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Post::getDetectionStatus, DETECTION_SUCCESS)  // 审核通过
                .eq(Post::getHasDelete,0);       //没有被删除
        //tagId 不为空
        if (!Strings.isBlank(tagId)){
            queryWrapper.eq(Post::getTagId,tagId);
        }
        Page<Post> list = this.page(page,queryWrapper);
        //获取的分页结果
        List<Post> records = list.getRecords();

//        List<String> userIds = records.stream()
//                .map(post -> post.getUid())
//                .collect(Collectors.toList());
        //获取作者
//        Object authors = rvcUserServiceFeignClient.list(userIds).getData();

        //遍历postvo
            //插入作者
            //如果uuid 不为空 去关系表进行查询 是否点赞和收藏

        List<Post> truncatedPosts = records.stream()
                .peek(post -> {
                    String content = post.getContent();
                    if (content != null && content.length() > 200) {
                        post.setContent(content.substring(0, 200));
                    }
                })
                .collect(Collectors.toList());

        List<PostVo> postVos = BeanCopyUtils.copyBeanList(truncatedPosts, PostVo.class);
        //如果uuid 不为空 去关系表进行查询 是否点赞和收藏
        if (!Strings.isBlank(uuid)){
            for (PostVo postVo : postVos) {
                LambdaQueryWrapper<CollectPost> collectPostQueryWrapper = new LambdaQueryWrapper<>();
                collectPostQueryWrapper.eq(CollectPost::getUid, uuid);
                LambdaQueryWrapper<LikePost> likePostQueryWrapper = new LambdaQueryWrapper<>();
                likePostQueryWrapper.eq(LikePost::getUid, uuid);

                boolean collect = collectPostMapper.selectCount(collectPostQueryWrapper) > 0;
                boolean like = likePostMapper.selectCount(likePostQueryWrapper) > 0;
                postVo.setLike(like);
                postVo.setCollect(collect);

                Object data = rvcUserServiceFeignClient.one(uuid).getData();
                UserInfoVO userInfoVO = JSON.parseObject(JSON.toJSONString(data), UserInfoVO.class);
                postVo.setUserInfoVO(userInfoVO);


                //获取user和tag

            }
        }

//         排序
//        SortStrategy sortStrategy = strategyMap.get(params.getData());
//        sortStrategy.sort(postVos);


        return postVos;

    }

    @Override
    public PostVo details(String postId) {

        /**
         * 模拟获取uuid
         */
        String uuid = "1734216713637244929";


//        更具id获取数据
        Post post = this.getById(postId);
        PostVo postVo = BeanCopyUtils.copyBean(post, PostVo.class);

        //获取作者和tag
        Object data = rvcUserServiceFeignClient.one(post.getUid()).getData();
        UserInfoVO userInfoVO = JSON.parseObject(JSON.toJSONString(data), UserInfoVO.class);
        postVo.setUserInfoVO(userInfoVO);

        PostType postType = postTypeMapper.selectById(post.getTagId());
        postVo.setPostType(postType);


//        如果用户未登录直接返回vo对象
        if (Objects.isNull(uuid)){
            return postVo;
        }


//        异步 帖子浏览次数+1（1分钟内浏览 浏览次数不加1 并且更新上次浏览时间）
        this.executor.execute(() -> watchPost(uuid,postId));

        //去关系表查看用户是否点赞  收藏
        LambdaQueryWrapper<CollectPost> collectPostQueryWrapper = new LambdaQueryWrapper<>();
        collectPostQueryWrapper.eq(CollectPost::getUid,uuid);
        LambdaQueryWrapper<LikePost> likePostQueryWrapper = new LambdaQueryWrapper<>();
        likePostQueryWrapper.eq(LikePost::getUid,uuid);
        boolean collect = collectPostMapper.selectCount(collectPostQueryWrapper) >0;
        boolean   like =   likePostMapper.selectCount(  likePostQueryWrapper) >0;

//        封装vo对象并返回
        postVo.setLike(like);
        postVo.setCollect(collect);

        return postVo;
    }

    @Override
    public String cover(String coverUrl) {
        /**
         * 判断用户是否登录
         */

//        数据库添加记录
        String coverId = Uuid.getUuid();
        Cover cover = Cover.builder()
                .coverId(coverId)
                .detectionStatus(UN_DETECTION)
                .coverUrl(coverUrl)
                .build();
        coverMapper.insert(cover);


        //       提交审核任务
        DetectionTaskDto textDetectionTaskDto = DetectionTaskDto.builder()
                .id(coverId)
                .content(coverUrl)
                .name("post_cover")
                .build();

        ProducerHandler producerHandler = BeanUtils.getBean(ProducerHandler.class);
        producerHandler.submit(textDetectionTaskDto,"image");

        return coverId;
    }

    @Override
    public void favorite(CoinDto coinDto) {
//        模拟获取uid
        String uid = "1";

        /**
         * 判断用户和评论是否存在
         */
        //1、点赞    添加关系表中的记录       post表 like_num +1
        //0、取消点赞    删除关系表中的记录       post表 like_num -1
        if (coinDto.getType().equals("1")){
            String uuid = Uuid.getUuid();
            LikePost likePost = new LikePost(uuid,  coinDto.getId(),uid);
            try {
                likePostMapper.insert(likePost);
            } catch (Exception e) {
                throw new RuntimeException("不允许重复点赞");
            }
            LambdaUpdateWrapper<Post> updateWrapper = Wrappers.<Post>lambdaUpdate()
                    .eq(Post::getPostId, coinDto.getId())
                    .setSql("like_num = like_num + 1");
            postMapper.update(null,updateWrapper);
        } else if (coinDto.getType().equals("0")) {
            LambdaQueryWrapper<LikePost> likePostLambdaQueryWrapper = new LambdaQueryWrapper<>();
            likePostLambdaQueryWrapper.eq(LikePost::getUid,uid)
                            .eq(LikePost::getPostId,coinDto.getId());
            int delete = likePostMapper.delete(likePostLambdaQueryWrapper);
              if (delete == 0){
                  throw new RuntimeException("不允许重复取消点赞");
              }
            LambdaUpdateWrapper<Post> updateWrapper = Wrappers.<Post>lambdaUpdate()
                    .eq(Post::getPostId, coinDto.getId())
                    .setSql("like_num = like_num - 1");
            postMapper.update(null,updateWrapper);
        }
    }

    @Override
    public void collection(CoinDto coinDto) {
        String uid = "1";
        /**
         * 判断用户和评论是否存在
         */
        //1、收藏    添加关系表中的记录       post表 colletc_num +1
        //0、取消收藏    删除关系表中的记录       post表 colletc_num -1
        if (coinDto.getType().equals("1")){
            String uuid = Uuid.getUuid();
            CollectPost collectPost = new CollectPost(coinDto.getId(), uid, uuid);
            try {
                collectPostMapper.insert(collectPost);
            } catch (Exception e) {
                throw new RuntimeException("操作失败");
            }
            LambdaUpdateWrapper<Post> updateWrapper = Wrappers.<Post>lambdaUpdate()
                    .eq(Post::getPostId, coinDto.getId())
                    .setSql("collect_num = collect_num + 1");
            postMapper.update(null,updateWrapper);
        } else if (coinDto.getType().equals("0")) {
            LambdaQueryWrapper<CollectPost> likePostLambdaQueryWrapper = new LambdaQueryWrapper<>();
            likePostLambdaQueryWrapper.eq(CollectPost::getUid,uid)
                    .eq(CollectPost::getPostId,coinDto.getId());
            int delete = collectPostMapper.delete(likePostLambdaQueryWrapper);
            if (delete == 0){
                throw new RuntimeException("操作失败");
            }
            LambdaUpdateWrapper<Post> updateWrapper = Wrappers.<Post>lambdaUpdate()
                    .eq(Post::getPostId, coinDto.getId())
                    .setSql("collect_num = collect_num - 1");
            postMapper.update(null,updateWrapper);
        }
    }

    @Override
    public void delete(String postId) {
        Post post = Post.builder()
                .postId(postId)
                .hasDelete(1)
                .build();
        System.out.println(post);
        try {
            postMapper.updateById(post);
        } catch (Exception e) {
            throw new RuntimeException("记录不存在");
        }
    }

    @Override
    public void add(PostDto postDto) {
/**
 * 模拟获取userid
 */
        String userid = "1734216713637244929";


        String uuid = Uuid.getUuid();
        Post post = BeanCopyUtils.copyBean(postDto, Post.class);
        post.setPostId(uuid);
        post.setUid(userid);
        post.setHasDelete(0);
        save(post);

        //更新cover表映射
        Cover cover = Cover.builder()
                .postId(uuid)
                .coverId(post.getCoverId())
                .build();

        coverMapper.updateById(cover);

        DetectionTaskDto textDetectionTaskDto = DetectionTaskDto.builder()
                .id(uuid)
                .content(post.getContent())
                .name("post.text")
                .build();
        ProducerHandler producerHandler = BeanUtils.getBean(ProducerHandler.class);
        producerHandler.submit(textDetectionTaskDto,"text");
    }

    @Override
    public void update(PostDto postDto) {

//        String uuid = Uuid.getUuid();
//        Post post = BeanCopyUtils.copyBean(postDto, Post.class);
//        post.setPostId(uuid);
//        save(post);
//
//        //更新cover表映射
//        Cover cover = Cover.builder()
//                .postId(uuid)
//                .coverId(post.getCoverId())
//                .build();
//
//        coverMapper.updateById(cover);
//
//
//
//        //对内容进行审核
//        //        审核
//        DetectionTaskDto textDetectionTaskDto = DetectionTaskDto.builder()
//                .id(uuid)
//                .content(post.getContent())
//                .name("post.text")
//                .build();
//
//        ProducerHandler producerHandler = BeanUtils.getBean(ProducerHandler.class);
//        producerHandler.submit(textDetectionTaskDto,"text");
    }

    @Override
    public List<PostVo> userFavorite(PageInfo<String> params) {

        String uuid = "1";

        Integer pageNum = params.getPage();
        Integer pageSize = params.getLimit();
        Page<LikePost> page = new Page<>(pageNum,pageSize);
        LambdaQueryWrapper<LikePost> likePostLambdaQueryWrapper = new LambdaQueryWrapper<>();
        likePostLambdaQueryWrapper.eq(LikePost::getUid, uuid);
        Page<LikePost> likePostPage = likePostMapper.selectPage(page, likePostLambdaQueryWrapper);
        List<String> collect = likePostPage.getRecords().stream()
                .map(likePost -> likePost.getPostId())
                .collect(Collectors.toList());

        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("post_id",collect);
        List<Post> posts = postMapper.selectList(queryWrapper);

        List<PostVo> postVos = BeanCopyUtils.copyBeanList(posts, PostVo.class);

        //对帖子内容进行限制（200字以内）
        List<PostVo> truncatedPosts = postVos.stream()
                .peek(post -> {
                    String content = post.getContent();
                    if (content != null && content.length() > 200) {
                        post.setContent(content.substring(0, 200));
                    }
                })
                .collect(Collectors.toList());

        return truncatedPosts;
    }

    @Override
    public List<PostVo> userCollect(PageInfo<String> params) {

        String uuid = "1";

        Integer pageNum = params.getPage();
        Integer pageSize = params.getLimit();
        Page<CollectPost> page = new Page<>(pageNum,pageSize);
        LambdaQueryWrapper<CollectPost> likePostLambdaQueryWrapper = new LambdaQueryWrapper<>();
        likePostLambdaQueryWrapper.eq(CollectPost::getUid, uuid);
        Page<CollectPost> likePostPage = collectPostMapper.selectPage(page, likePostLambdaQueryWrapper);
        List<String> collect = likePostPage.getRecords().stream()
                .map(likePost -> likePost.getPostId())
                .collect(Collectors.toList());

        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("post_id",collect);
        List<Post> posts = postMapper.selectList(queryWrapper);

        List<PostVo> postVos = BeanCopyUtils.copyBeanList(posts, PostVo.class);

        //对帖子内容进行限制（200字以内）
        List<PostVo> truncatedPosts = postVos.stream()
                .peek(post -> {
                    String content = post.getContent();
                    if (content != null && content.length() > 200) {
                        post.setContent(content.substring(0, 200));
                    }
                })
                .collect(Collectors.toList());

        return truncatedPosts;
    }

    @Override
    public List<PostVo> userCreate(PageInfo<String> params) {
        String uuid = "1";

        Integer pageNum = params.getPage();
        Integer pageSize = params.getLimit();
        Page<Post> page = new Page<>(pageNum,pageSize);
        LambdaQueryWrapper<Post> likePostLambdaQueryWrapper = new LambdaQueryWrapper<>();
        likePostLambdaQueryWrapper.eq(Post::getUid, uuid);
        Page<Post> likePostPage = postMapper.selectPage(page, likePostLambdaQueryWrapper);
        List<String> collect = likePostPage.getRecords().stream()
                .map(likePost -> likePost.getPostId())
                .collect(Collectors.toList());

        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("post_id",collect);
        List<Post> posts = postMapper.selectList(queryWrapper);

        List<PostVo> postVos = BeanCopyUtils.copyBeanList(posts, PostVo.class);

        //对帖子内容进行限制（200字以内）
        List<PostVo> truncatedPosts = postVos.stream()
                .peek(post -> {
                    String content = post.getContent();
                    if (content != null && content.length() > 200) {
                        post.setContent(content.substring(0, 200));
                    }
                })
                .collect(Collectors.toList());

        return truncatedPosts;
    }


    public void watchPost(String userId,String postId){

//        从redis获取上次浏览时间
        Object cacheObject = redisCache.getCacheObject(RVC_COMMUNICATION_POST_WATCH + ":" + userId);
        if (Objects.isNull(cacheObject)){
//            如果没有获取到数据 说明是第一次浏览  保存标记到redis 设置失效时间为一小时  浏览次数直接+1
            redisCache.setCacheObject(RVC_COMMUNICATION_POST_WATCH + ":" + userId,1,1, TimeUnit.HOURS);
//            浏览量加1
            LambdaUpdateWrapper<Post> updateWrapper = Wrappers.<Post>lambdaUpdate()
                    .eq(Post::getPostId, postId)
                    .setSql("watch_num = watch_num + 1");
            postMapper.update(null,updateWrapper);
            return;
        }

//    不为空更新redis失效时间
        redisCache.setCacheObject(RVC_COMMUNICATION_POST_WATCH + ":" + userId,1,1, TimeUnit.HOURS);
            /**
             * 是否需要记录浏览信息
             */
        }
//        if (now.compareTo(oneHourBefore))
}