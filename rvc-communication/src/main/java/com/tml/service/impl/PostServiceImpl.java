package com.tml.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tml.client.FileServiceClient;
import com.tml.client.UserServiceClient;
import com.tml.designpattern.chain.ext.*;
import com.tml.domain.dto.CoinDto;
import com.tml.domain.dto.CoverDto;
import com.tml.domain.dto.PageInfo;
import com.tml.domain.dto.PostDto;
import com.tml.domain.entity.*;
import com.tml.domain.vo.ScoreListVo;
import com.tml.domain.vo.TaskDataRes;
import com.tml.handler.exception.SystemException;
import com.tml.mapper.common.CommonMapper;
import com.tml.mapper.post.*;
import com.tml.pojo.DTO.ReceiveUploadFileDTO;
import com.tml.pojo.VO.UploadModelForm;
import com.tml.pojo.VO.UserInfoVO;

import com.tml.domain.vo.PostVo;
import com.tml.service.PostService;
import com.tml.designpattern.strategy.SortStrategy;
import com.tml.designpattern.strategy.impl.LikeSortStrategy;
import com.tml.designpattern.strategy.impl.TimeSortStrategy;
import com.tml.designpattern.strategy.impl.ViewSortStrategy;
import com.tml.utils.*;
import io.github.common.web.Result;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


import static com.tml.constant.CommonConstant.IMG_TYPE_LIST;
import static com.tml.constant.DBConstant.RVC_COMMUNICATION_POST_TYPE;
import static com.tml.constant.DBConstant.RVC_COMMUNICATION_POST_WATCH;
import static com.tml.constant.DetectionConstants.DETECTION_SUCCESS;
import static com.tml.constant.DetectionConstants.UN_DETECTION;
import static com.tml.constant.enums.AppHttpCodeEnum.*;

/**
 * @NAME: PostServiceImpl
 * @USER: yuech
 * @Description:
 * @DATE: 2023/12/5
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings({"all"})
public class PostServiceImpl implements PostService {

    private final CollectPostMapper collectPostMapper;
    private final LikePostMapper likePostMapper;
    private final CoverMapper coverMapper;
    private final PostMapper postMapper;
    private final ThreadPoolTaskExecutor executor;
    private final UserServiceClient rvcUserServiceFeignClient;
    private final RedisCache redisCache;
    private final PostTypeMapper postTypeMapper;
    private final FileServiceClient fileServiceClient;
    private final UserServiceClient userServiceClient;

    //责任链
    private final UserExistApproveChain userExistApproveChain;
    private final PostExistApproveChain postExistApproveChain;
    private final LastStepApproveChain lastStepApproveChain;
    private final TagExistApproveChain tagExistApproveChain;
    private final CoverExistApproveChain coverExistApproveChain;

    private final Map<String, String> strategyMap = new HashMap<>();
    {
        strategyMap.put("1", "create_at");
        strategyMap.put("2", "like_num");
        strategyMap.put("3", "watch_num");
        strategyMap.put("4", "collect_num");
    }

    @Override
    public List<PostVo> list(String uid,Integer pageNum,Integer pageSize,String order,String tagId){
        if (!Strings.isBlank(uid)){
            userExistApproveChain.setNext(uid,lastStepApproveChain);
            userExistApproveChain.approve();
        }
        //分页
//        LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Post::getDetectionStatus, DETECTION_SUCCESS)  // 审核通过
//                .eq(Post::getHasDelete,0)//没有被删除
//                .orderBy(!Strings.isBlank(order),)
//                .eq(!Strings.isBlank(tagId),Post::getTagId,tagId);//tagId 不为空  更具tagId 查询



        QueryWrapper<Post> postQueryWrapper = new QueryWrapper<>();
        postQueryWrapper.eq("detection_status",DETECTION_SUCCESS)
                .eq("has_delete",0)
                .eq(!Strings.isBlank(tagId),"tag_id",tagId)//tagId 不为空  更具tagId 查询
                .orderByDesc(strategyMap.get(order))
                .orderByDesc("post_id");

//                .orderBy(true, false, "");


        Page<Post> list = postMapper.selectPage(new Page<>(pageNum,pageSize),postQueryWrapper);
        //获取的分页结果
        List<Post> records = list.getRecords();
        List<PostVo> postVos = BeanCopyUtils.copyBeanList(records, PostVo.class);
        //        //获取作者
        List<String> userIds = records.stream()
                .map(post -> post.getUid())
                .collect(Collectors.toList());
        Result<Map<String, UserInfoVO>> res = rvcUserServiceFeignClient.list(userIds);
        Map<String, UserInfoVO> data = res.getData();
        //获取tag
        List<Object> cacheList = redisCache.getCacheList(RVC_COMMUNICATION_POST_TYPE);
        String jsonString = JSON.toJSONString(cacheList);
        List<PostType> postTypes = JSONArray.parseArray(jsonString, PostType.class);
        Map<String, PostType> postTypeCollect = postTypes.stream()
                .collect(Collectors.toMap(postType -> postType.getId(), postType -> postType));
        //遍历postvo
        //插入作者
        //插入tag
        //如果uuid 不为空 去关系表进行查询 是否点赞和收藏
        for (int i=0;i<postVos.size();i++){
            PostVo postVo = postVos.get(i);
            //插入作者
            postVo.setAuthor(data.get(records.get(i).getUid()));
            //插入tag
            postVo.setPostType(postTypeCollect.get(records.get(i).getTagId()));
            //获取封面
            Cover cover = coverMapper.selectById(records.get(i).getCoverId());
            postVo.setCover(cover.getCoverUrl());
            //如果uuid 不为空 去关系表进行查询 是否点赞和收藏
            if (!Strings.isBlank(uid)){
                postVo.setLike(hasFavorite(uid,postVo.getPostId()));
                postVo.setCollect(hasCollect(uid,postVo.getPostId()));
            }
        }
//        //         排序
//        SortStrategy sortStrategy = strategyMap.get(order);
//        sortStrategy.sort(postVos);
        return postVos;

    }

    @Override
    public PostVo details(String postId,String uid) {
        if (!Strings.isBlank(uid)){
            userExistApproveChain.setNext(uid,lastStepApproveChain);
            userExistApproveChain.approve();
        }
        LambdaQueryWrapper<Post> postLambdaQueryWrapper = new LambdaQueryWrapper<>();
        postLambdaQueryWrapper.eq(Post::getPostId,postId)
                .eq(Post::getHasDelete,0)  //没有删除
                .eq(Post::getDetectionStatus,1);    //审核通过
        Post post = postMapper.selectOne(postLambdaQueryWrapper);
        if (Objects.isNull(post)){
            throw new SystemException(POST_ERROR);
        }
        PostVo postVo = BeanCopyUtils.copyBean(post, PostVo.class);
        //获取作者和tag 还有cover
        postVo.setAuthor(getUserInfo(post.getUid()));
        postVo.setPostType(postTypeMapper.selectById(post.getTagId()));
        postVo.setCover(coverMapper.selectById(post.getCoverId()).getCoverUrl());
        postVo.setCoverId(post.getCoverId());
//        如果用户未登录直接返回vo对象
        if (Objects.isNull(uid)){
            return postVo;
        }
//        异步 帖子浏览次数+1（1分钟内浏览 浏览次数不加1 并且更新上次浏览时间）
        this.executor.execute(() -> watchPost(uid,postId));
        postVo.setLike( hasFavorite(uid,postId));
        postVo.setCollect(hasCollect(uid,postId));
        return postVo;
    }

    @Override
    @Transactional
    public void favorite(CoinDto coinDto, String uid) {
        userExistApproveChain.setNext(uid,postExistApproveChain);
        postExistApproveChain.setNext(coinDto.getId(),lastStepApproveChain);
        userExistApproveChain.approve();

        //1、点赞    添加关系表中的记录       post表 like_num +1
        //0、取消点赞    删除关系表中的记录       post表 like_num -1
        if (coinDto.getType().equals("1")){
            String uuid = Uuid.getUuid();
            LikePost likePost = new LikePost(uuid,  coinDto.getId(),uid);
            try {
                likePostMapper.insert(likePost);
            } catch (Exception e) {
                throw new SystemException(FAVORITE_ERROR);
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
                throw new SystemException(SYSTEM_ERROR);
            }
            LambdaUpdateWrapper<Post> updateWrapper = Wrappers.<Post>lambdaUpdate()
                    .eq(Post::getPostId, coinDto.getId())
                    .setSql("like_num = like_num - 1");
            postMapper.update(null,updateWrapper);
        }else {
            throw new SystemException(TYPE_ERROR);
        }
    }

    @Override
    @Transactional
    public void collection(CoinDto coinDto,String uid) {
        userExistApproveChain.setNext(uid,postExistApproveChain);
        postExistApproveChain.setNext(coinDto.getId(),lastStepApproveChain);
        userExistApproveChain.approve();
        //1、收藏    添加关系表中的记录       post表 colletc_num +1
        //0、取消收藏    删除关系表中的记录       post表 colletc_num -1
        if (coinDto.getType().equals("1")){
            String uuid = Uuid.getUuid();
            CollectPost collectPost = new CollectPost(coinDto.getId(), uid, uuid);
            try {
                collectPostMapper.insert(collectPost);
            } catch (Exception e) {
                throw new SystemException(COLLECT_ERROR);
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
                throw new SystemException(SYSTEM_ERROR);
            }
            LambdaUpdateWrapper<Post> updateWrapper = Wrappers.<Post>lambdaUpdate()
                    .eq(Post::getPostId, coinDto.getId())
                    .setSql("collect_num = collect_num - 1");
            postMapper.update(null,updateWrapper);
        }else {
            throw new SystemException(TYPE_ERROR);
        }
    }

    @Override
    public void delete(String postId,String uid) {
        userExistApproveChain.setNext(uid,postExistApproveChain);
        postExistApproveChain.setNext(postId,lastStepApproveChain);
        userExistApproveChain.approve();
        LambdaQueryWrapper<Post> eq = new LambdaQueryWrapper<Post>().eq(Post::getPostId, postId)
                .eq(Post::getUid, uid);
        if (postMapper.selectCount(eq)<1){
            throw new RuntimeException("无权限操作");
        }
        postMapper.updateById(Post.builder()
                .postId(postId)
                .hasDelete(1)
                .build());
    }

    @Override
    @Transactional
    public String add(PostDto postDto, String uid) {
        userExistApproveChain.setNext(uid,tagExistApproveChain);
        tagExistApproveChain.setNext(postDto.getTagId(),coverExistApproveChain);
        coverExistApproveChain.setNext(postDto.getCoverId(),lastStepApproveChain);
        userExistApproveChain.approve();

        String uuid = Strings.isBlank(postDto.getPostId())?Uuid.getUuid():postDto.getPostId();
        Post post = BeanCopyUtils.copyBean(postDto, Post.class);
        post.setPostId(uuid);
        //关闭审核  将审核状态修改为审核成功
        post.setDetectionStatus(DETECTION_SUCCESS);
        post.setUid(uid);
        //如果能从数据库找到这个帖子的你的就修改   不能找到就创建
        if (postMapper.selectCount(new LambdaQueryWrapper<Post>().eq(Post::getUid,uid).eq(Post::getPostId,postDto.getPostId()))<1){
            postMapper.insert(post);
        }else {
            postMapper.updateById(post);
        }
        coverMapper.updateById(Cover.builder()
                .postId(uuid)
                .coverId(postDto.getCoverId())
                .build());
        return uuid;
    }

    @Override
    public List<PostVo> userFavorite(String uid,Integer pageNum,Integer pageSize,String order) {
        //  责任链
        userExistApproveChain.setNext(uid,lastStepApproveChain);
        userExistApproveChain.approve();
        //获取用户喜欢的postid
        LambdaQueryWrapper<LikePost> likePostLambdaQueryWrapper = new LambdaQueryWrapper<>();
        likePostLambdaQueryWrapper.eq(LikePost::getUid, uid);
        Page<LikePost> likePostPage = likePostMapper.selectPage(new Page<>(pageNum,pageSize), likePostLambdaQueryWrapper);
        List<LikePost> records = likePostPage.getRecords();
        if (records.size() == 0){
            throw new SystemException(NOT_FAVORITE_ERROR);
        }
        List<String> collect = likePostPage.getRecords().stream()
                .map(likePost -> likePost.getPostId())
                .collect(Collectors.toList());
//更具id获取post
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("post_id",collect)
                .eq("detection_status",1)
                .eq("has_delete",0);
        List<Post> posts = postMapper.selectList(queryWrapper);
//封装信息
        List<PostVo> postSimpleVos = BeanCopyUtils.copyBeanList(posts, PostVo.class);
        //获取tag  封装为map
        List<Object> cacheList = redisCache.getCacheList(RVC_COMMUNICATION_POST_TYPE);
        String jsonString = JSON.toJSONString(cacheList);
        List<PostType> postTypes = JSONArray.parseArray(jsonString, PostType.class);
        Map<String, PostType> postTypeCollect = postTypes.stream()
                .collect(Collectors.toMap(postType -> postType.getId(), postType -> postType));
        for (int i=0;i<postSimpleVos.size();i++){
            Post post = posts.get(i);
            PostVo postSimpleVo = postSimpleVos.get(i);
            String content = postSimpleVo.getContent();
            //对帖子内容进行限制（200字以内）
            if (content.length()>200){
                postSimpleVo.setContent(content.substring(0, 200));
            }
            //封面
            postSimpleVo.setCover(coverMapper.selectById( post.getCoverId()).getCoverUrl());
            //post类型
            postSimpleVo.setPostType(postTypeCollect.get(posts.get(i).getTagId()));
            //作者
            //获取用户信息
            UserInfoVO data = getUserInfo(post.getUid());
            postSimpleVo.setAuthor(data);
            postSimpleVo.setLike(hasFavorite(uid,post.getPostId()));
            postSimpleVo.setCollect(hasCollect(uid,post.getPostId()));
        }
        return postSimpleVos;
    }

    @Override
    public List<PostVo> userCollect(String uid,Integer pageNum,Integer pageSize,String order){
        //  责任链
        userExistApproveChain.setNext(uid,lastStepApproveChain);
        userExistApproveChain.approve();

        //获取用户收藏的postid
        LambdaQueryWrapper<CollectPost> likePostLambdaQueryWrapper = new LambdaQueryWrapper<>();
        likePostLambdaQueryWrapper.eq(CollectPost::getUid, uid);
        Page<CollectPost> likePostPage = collectPostMapper.selectPage(new Page<>(pageNum,pageSize), likePostLambdaQueryWrapper);
        List<CollectPost> records = likePostPage.getRecords();
        if (records.size() == 0){
            throw new SystemException(NOT_FAVORITE_ERROR);
        }
        List<String> collect = likePostPage.getRecords().stream()
                .map(likePost -> likePost.getPostId())
                .collect(Collectors.toList());
//更具id获取post
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("post_id",collect)
                .eq("detection_status",1)
                .eq("has_delete",0);
        List<Post> posts = postMapper.selectList(queryWrapper);
//封装信息
        List<PostVo> postSimpleVos = BeanCopyUtils.copyBeanList(posts, PostVo.class);
        //获取tag  封装为map
        List<Object> cacheList = redisCache.getCacheList(RVC_COMMUNICATION_POST_TYPE);
        String jsonString = JSON.toJSONString(cacheList);
        List<PostType> postTypes = JSONArray.parseArray(jsonString, PostType.class);
        Map<String, PostType> postTypeCollect = postTypes.stream()
                .collect(Collectors.toMap(postType -> postType.getId(), postType -> postType));
        for (int i=0;i<postSimpleVos.size();i++){
            Post post = posts.get(i);
            PostVo postSimpleVo = postSimpleVos.get(i);
            String content = postSimpleVo.getContent();
            //对帖子内容进行限制（200字以内）
            if (content.length()>200){
                postSimpleVo.setContent(content.substring(0, 200));
            }
            //封面
            postSimpleVo.setCover(coverMapper.selectById( post.getCoverId()).getCoverUrl());
            //post类型
            postSimpleVo.setPostType(postTypeCollect.get(posts.get(i).getTagId()));
            //作者
            //获取用户信息
            UserInfoVO data = getUserInfo(post.getUid());
            postSimpleVo.setAuthor(data);
            postSimpleVo.setLike(hasFavorite(uid,post.getPostId()));
            postSimpleVo.setCollect(hasCollect(uid,post.getPostId()));
        }
        return postSimpleVos;
    }

    @Override
    public List<PostVo> userCreate(String uid,Integer pageNum,Integer pageSize,String order) {
        //  责任链
        userExistApproveChain.setNext(uid,lastStepApproveChain);
        userExistApproveChain.approve();
        //获取用户信息
        UserInfoVO data = getUserInfo(uid);
        //获取用户创建的postid
        LambdaQueryWrapper<Post> likePostLambdaQueryWrapper = new LambdaQueryWrapper<>();
        likePostLambdaQueryWrapper.eq(Post::getUid, uid)
                .eq(Post::getDetectionStatus,1)
                .eq(Post::getHasDelete,0);
        Page<Post> likePostPage = postMapper.selectPage(new Page<>(pageNum,pageSize), likePostLambdaQueryWrapper);
        List<Post> posts = likePostPage.getRecords();
        if (posts.size() == 0){
            throw new SystemException(NOT_FAVORITE_ERROR);
        }
//        List<String> collect = likePostPage.getRecords().stream()
//                .map(likePost -> likePost.getPostId())
//                .collect(Collectors.toList());
//更具id获取post
//        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
//        queryWrapper.in("post_id",collect)
//                .eq("detection_status",1)
//                .eq("has_delete",0);
//        List<Post> posts = postMapper.selectList(queryWrapper);
//封装信息
        List<PostVo> postSimpleVos = BeanCopyUtils.copyBeanList(posts, PostVo.class);
        //获取tag  封装为map
        List<Object> cacheList = redisCache.getCacheList(RVC_COMMUNICATION_POST_TYPE);
        String jsonString = JSON.toJSONString(cacheList);
        List<PostType> postTypes = JSONArray.parseArray(jsonString, PostType.class);
        Map<String, PostType> postTypeCollect = postTypes.stream()
                .collect(Collectors.toMap(postType -> postType.getId(), postType -> postType));
        for (int i=0;i<postSimpleVos.size();i++){
            Post post = posts.get(i);
            PostVo postSimpleVo = postSimpleVos.get(i);
            String content = postSimpleVo.getContent();
            //对帖子内容进行限制（200字以内）
            if (content.length()>200){
                postSimpleVo.setContent(content.substring(0, 200));
            }
            //封面
            postSimpleVo.setCover(coverMapper.selectById( post.getCoverId()).getCoverUrl());
            //post类型
            postSimpleVo.setPostType(postTypeCollect.get(posts.get(i).getTagId()));
            //作者
            postSimpleVo.setAuthor(data);
            postSimpleVo.setLike(hasFavorite(uid,post.getPostId()));
            postSimpleVo.setCollect(hasCollect(uid,post.getPostId()));
        }
        return postSimpleVos;
    }

    @Override
    public String updUserProfile(MultipartFile profile,String uid) throws IOException {
        Object res = userServiceClient.exist(uid).getData();
        if (Objects.isNull(res)){
            throw new SystemException(NEED_LOGIN);
        }
        //判断后缀名
        String fileSuffix = profile.getOriginalFilename().substring(profile.getOriginalFilename().lastIndexOf("."));
        if (!Arrays.stream(IMG_TYPE_LIST).anyMatch(
                type -> fileSuffix.equals(type))
        ){
            throw new SystemException(TYPE_ERROR);
        }

        byte[] uploadBytes = profile.getBytes();
        UploadModelForm build = UploadModelForm.builder()
                .bucket("rvc1")
                .file(profile)
                .md5(MD5Util.getMD5(uploadBytes.toString()))
                .path("rvc/image3")
                .build();
        com.tml.pojo.Result<ReceiveUploadFileDTO> receiveUploadFileDTOResult = null;
        try {
            receiveUploadFileDTOResult = fileServiceClient.uploadModel(build);
        } catch (Exception e) {
            throw new SystemException(SERVICE_ERROR);
        }
        ReceiveUploadFileDTO data = receiveUploadFileDTOResult.getData();

        return data.getUrl();
    }

    @Override
    public String coverUrl(CoverDto coverDto) {
        String uuid = Uuid.getUuid();
        Cover build = Cover.builder()
                .uid(coverDto.getUid())
                .coverUrl(coverDto.getCoverUrl())
                .createAt(LocalDate.now())
                /**
                 * DETECTION_SUCCESS
                 */
                .detectionStatus(UN_DETECTION)
                .coverId(uuid)
                .build();
        coverMapper.insert(build);
        return uuid;
    }

    @Override
    public TaskDataRes scoreList() {
        // id和score   list
        List<Post> scoreList = postMapper.getScoreList();
        //获取id和score
        List<ScoreListVo> scoreListVoList = scoreList.stream()
                .map(post -> new ScoreListVo(post.getPostId(), post.getTotalScore()))
                .collect(Collectors.toList());
        //获取map
        List<String> ids = scoreListVoList.stream()
                .map(o -> o.getId())
                .collect(Collectors.toList());

        QueryWrapper<Post> userQuery = new QueryWrapper<>();
        userQuery.in("post_id", ids);

        List<Post> posts = postMapper.selectList(userQuery);
        Map<String, Post> collect = posts.stream()
                .collect(Collectors.toMap(Post::getPostId, post -> post));

        return new TaskDataRes(collect,scoreListVoList);
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

    public UserInfoVO getUserInfo(String userId){
        //调用用户服务获取评论创建人的信息
        UserInfoVO data = null;
        try {
            data = userServiceClient.one(userId).getData();
        } catch (Exception e) {
            throw new RuntimeException("用户服务调用异常");
        }
        return data;
    }

    public boolean hasFavorite(String uid,String postId){
        LambdaQueryWrapper<LikePost> likePostQueryWrapper = new LambdaQueryWrapper<>();
        likePostQueryWrapper.eq(LikePost::getUid,uid)
                .eq(LikePost::getPostId,postId);
        boolean   like =   likePostMapper.selectCount(  likePostQueryWrapper) >0;
        return like;
    }
    public boolean hasCollect(String uid,String postId){
        LambdaQueryWrapper<CollectPost> collectPostQueryWrapper = new LambdaQueryWrapper<>();
        collectPostQueryWrapper.eq(CollectPost::getUid,uid)
                .eq(CollectPost::getPostId,postId);
        boolean collect = collectPostMapper.selectCount(collectPostQueryWrapper) >0;
        return collect;
    }
}
