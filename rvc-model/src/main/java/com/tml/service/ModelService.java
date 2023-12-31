package com.tml.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.tml.pojo.DTO.ReceiveUploadFileDTO;
import com.tml.pojo.VO.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Description
 * @Author welsir
 * @Date 2023/12/4 14:29
 */
public interface ModelService {

    Page<ModelVO> getModelList(String size, String page,String sortType,String uid);

    Page<ModelVO> getModelList(String type,String page,String size,String sortType,String uid);

    ModelVO queryOneModel(String modelId, String uid);

    void insertOneModel(ModelInsertVO model,String uid);

    String downloadModel(String modelId);

    Boolean editModelMsg(ModelUpdateFormVO modelUpdateFormVO);

    ReceiveUploadFileDTO uploadModel(MultipartFile file);

    ReceiveUploadFileDTO uploadImage(MultipartFile file);

    void insertRelative(String type,String modelId,String uid,String isClick);

    String insertLabel(String label,String uid);

    List<UserLikesModelVO> getUserLikesList(String uid);

    List<UserCollectionModelVO> getUserCollectionList(String uid);

    Boolean delSingleModel(String modelId);

    Page<ModelVO> queryUserModelList(String uid,String page,String limit);

    String commentModel(CommentFormVO commentFormVO,String uid);

    Boolean likeComment(String uid,String commentId,String type);

    Page<FirstCommentVO> queryFirstCommentList(String modelId, String page, String limit, String sortType,String uid);

    Page<SecondCommentVO> querySecondCommentList(String parentCommentId,String page,String limit,String sortType,String uid);

    Boolean userLikesModel(String status,String modelId,String uid);

    Boolean userCollectionModel(String status,String modelId,String uid);
}
