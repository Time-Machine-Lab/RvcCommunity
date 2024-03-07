package com.tml.service;

import com.tml.domain.dto.CoinDto;
import com.tml.domain.dto.CoverDto;
import com.tml.domain.dto.PageInfo;
import com.tml.domain.dto.PostDto;
import com.tml.domain.vo.PostVo;
import com.tml.domain.vo.TaskDataRes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PostService {
    List<PostVo> list(String uid,Integer page,Integer limit,String order,String tagId);

    PostVo details(String postId,String uid);


//    String cover(String coverUrl);

    void favorite(CoinDto coinDto,String uid);

    void collection(CoinDto coinDto,String uid);

    void delete(String postId,String uid);

    String add(PostDto postDto,String uid);

//    void update(PostDto postDto);

    List<PostVo> userFavorite(String uid,Integer page,Integer limit,String order);

    List<PostVo> userCollect(String uid,Integer page,Integer limit,String order);

    List<PostVo> userCreate(String uid,Integer page,Integer limit,String order);

    String updUserProfile(MultipartFile profile,String uid) throws IOException;

    String coverUrl(CoverDto coverDto);

    TaskDataRes scoreList();
}
