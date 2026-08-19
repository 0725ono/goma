package com.familyschedule.api.schedule.infrastructure.event;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA によるリポジトリ。メソッド名からクエリが自動生成される。 */
public interface EventJpaRepository extends JpaRepository<EventEntity, UUID> {

    List<EventEntity> findBySpaceId(UUID spaceId);
}
