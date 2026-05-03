package com.aegisops.backend.repository;

import com.aegisops.backend.entity.RunbookChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RunbookChunkRepository extends JpaRepository<RunbookChunk, UUID> {

    List<RunbookChunk> findByRunbookIdOrderByChunkIndexAsc(UUID runbookId);

    @Query("""
            select chunk
            from RunbookChunk chunk
            where lower(chunk.chunkText) like lower(concat('%', :term, '%'))
               or lower(chunk.runbook.title) like lower(concat('%', :term, '%'))
            order by case when lower(chunk.serviceName) = lower(:serviceName) then 0 else 1 end,
                     chunk.createdAt desc
            """)
    List<RunbookChunk> searchByKeywordPreferService(
            @Param("term") String term,
            @Param("serviceName") String serviceName
    );
}
