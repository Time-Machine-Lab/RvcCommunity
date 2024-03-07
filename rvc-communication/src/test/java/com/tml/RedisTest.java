package com.tml;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tml.client.CommunicationServiceClient;
import com.tml.domain.dto.PostDto;
import com.tml.domain.entity.Post;
import com.tml.domain.entity.PostType;
import com.tml.domain.vo.ScoreListVo;
import com.tml.mapper.post.PostMapper;
import com.tml.service.PostService;
import com.tml.utils.RedisCache;
import io.github.common.web.Result;
import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tml.constant.DBConstant.RVC_COMMUNICATION_POST_TYPE;
import static com.tml.constant.DBConstant.RVC_COMMUNICATION_POST_WATCH;

/**
 * @NAME: RedisTest
 * @USER: yuech
 * @Description:
 * @DATE: 2023/12/14
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
public class RedisTest {

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private StringEncryptor stringEncryptor;

    @Autowired
    private PostService postService;

    @Resource
    private PostMapper postMapper;

    @Resource
    private RedisTemplate redisTemplate;


    @Test
    public void TransactionalTest(){
        List<Post> scoreList = postMapper.getScoreList();
        List<ScoreListVo> scoreListVoList = scoreList.stream()
                .map(post -> new ScoreListVo(post.getPostId(), post.getTotalScore()))
                .collect(Collectors.toList());

        List<String> ids = scoreListVoList.stream()
                .map(o -> o.getId())
                .collect(Collectors.toList());


        QueryWrapper<Post> userQuery = new QueryWrapper<>();
        userQuery.in("post_id", ids);

        List<Post> posts = postMapper.selectList(userQuery);
        Map<String, Post> collect = posts.stream()
                .collect(Collectors.toMap(Post::getPostId, post -> post));


    }



    @Test
    public void insert(){

        for (int i = 1; i < 50; i++) {
            PostDto postDto = new PostDto();
            postDto.setTitle("测试帖子" + i);
            postDto.setContent("测试帖子" + i);
            postDto.setCoverId("1746188338123112448");
            postDto.setTagId("1");
            String uid = "1738846007402758145";

            postService.add(postDto,uid);
        }



    }

    @Test
    public void topTest(){
        PostDto postDto = new PostDto();
        postDto.setTitle("测试帖子");
        postDto.setContent("测试帖子");
        postDto.setCoverId("1746188338123112448");
        postDto.setTagId("1");

        ZSetOperations zSetOps = redisTemplate.opsForZSet();
        zSetOps.add("rvc:top:test", postDto, 1.0);

    }


    @Test
    public void  remove(){
        redisCache.deleteObject("rvc_communication_post_type");
    }

    @Test
    public void  addPostType(){
        ArrayList<PostType> postTypes = new ArrayList<>();
        postTypes.add(new PostType("1","https://p1.ssl.qhmsg.com/t01da032a5036754f87.jpg","谈天说地"));
        postTypes.add(new PostType("2","https://img1.baidu.com/it/u=1754140372,3255125879&fm=253&fmt=auto&app=138&f=JPEG?w=750&h=500","教教你的"));
        postTypes.add(new PostType("3","https://img0.baidu.com/it/u=835765349,4058962119&fm=253&fmt=auto&app=138&f=PNG?w=875&h=450","炼丹心得"));
        postTypes.add(new PostType("4","https://img2.baidu.com/it/u=4042164174,1682988452&fm=253&fmt=auto&app=138&f=JPEG?w=790&h=490","模型分析"));

        redisCache.setCacheList(RVC_COMMUNICATION_POST_TYPE,postTypes);
    }

    @Test
    public void  getPostType(){
        List<Object> cacheList = redisCache.getCacheList(RVC_COMMUNICATION_POST_TYPE);
        String jsonString = JSON.toJSONString(cacheList);
        List<PostType> postTypes = JSONArray.parseArray(jsonString, PostType.class);
        Map<String, PostType> collect = postTypes.stream()
                .collect(Collectors.toMap(postType -> postType.getId(), postType -> postType));
        System.out.println(collect);

    }



    @Test
    public void  getWatch(){
        Object cacheObject = redisCache.getCacheObject(RVC_COMMUNICATION_POST_WATCH + ":" + 1);
        System.out.println(cacheObject);
    }

    @Test
    public void  addWatch(){
        LocalDateTime now = LocalDateTime.now();
        redisCache.setCacheObject(RVC_COMMUNICATION_POST_WATCH + ":" + 1,now);
//        redisCache.setCacheObject(RVC_COMMUNICATION_POST_WATCH + ":" + 1,"1");
        System.out.println("over");

    }

    @Test
    public void test(){
        Object cacheObject = redisCache.getCacheObject(RVC_COMMUNICATION_POST_WATCH + ":" + 1);
        System.out.println(cacheObject);

    }



}