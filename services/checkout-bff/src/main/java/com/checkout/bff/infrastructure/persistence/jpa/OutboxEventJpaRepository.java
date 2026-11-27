package com.checkout.bff.infrastructure.persistence.jpa;

import com.checkout.bff.infrastructure.persistence.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.publishedAt IS NULL ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnpublishedEvents();

    @Query("SELECT o FROM OutboxEvent o WHERE o.publishedAt IS NULL ORDER BY o.createdAt ASC")
    List<OutboxEvent> findFirst100UnpublishedEvents();

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent o SET o.publishedAt = :now, o.retryCount = o.retryCount + 1, o.lastError = :error WHERE o.id = :id")
    void markAsFailed(@Param("id") Long id, @Param("now") LocalDateTime now, @Param("error") String error);

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent o SET o.publishedAt = :now WHERE o.id = :id")
    void markAsPublished(@Param("id") Long id, @Param("now") LocalDateTime now);
}