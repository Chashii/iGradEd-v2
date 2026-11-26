package com.igraded.news;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** What a person sees today: started, not ended, and for the whole school or their own department. */
    @Query("select a from Announcement a "
            + "where a.publishedOn <= :today "
            + "and (a.expiresOn is null or a.expiresOn >= :today) "
            + "and (a.departmentId is null or a.departmentId = :departmentId)")
    List<Announcement> visibleTo(@Param("today") LocalDate today, @Param("departmentId") Long departmentId);

    List<Announcement> findAllByOrderByPublishedOnDescIdDesc();
}
