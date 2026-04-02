package com.dage.rent.DAO.mysql;

import com.dage.rent.DTO.AttachmentFileDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository("mysqlAttachmentFileDAO")
public interface AttachmentFileDAO {
    int insert(AttachmentFileDTO file);
    AttachmentFileDTO findById(@Param("id") Long id);
    List<AttachmentFileDTO> findByIds(@Param("ids") List<Long> ids);
    int updateContractSeqByIds(@Param("ids") List<Long> ids, @Param("contractSeq") Integer contractSeq);
    int deleteById(@Param("id") Long id);
}
