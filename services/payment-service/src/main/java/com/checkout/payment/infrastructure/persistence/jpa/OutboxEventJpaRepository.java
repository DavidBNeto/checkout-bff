package com.checkout.payment.infrastructure.persistence.jpa;

import com.checkout.payment.infrastructure.persistence.entity.OutboxEvent;
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
    List<OutboxEvent> findFirst100UnpublishedEvents();

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent o SET o.publishedAt = :publishedAt WHERE o.id = :id")
    int markAsPublished(@Param("id") Long id, @Param("publishedAt") LocalDateTime publishedAt);

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent o SET o.publishedAt = :publishedAt, o.lastError = :lastError, o.retryCount = o.retryCount + 1 WHERE o.id = :id")
    int markAsFailed(@Param("id") Long id, @Param("publishedAt") LocalDateTime publishedAt, @Param("lastError") String lastError);
}