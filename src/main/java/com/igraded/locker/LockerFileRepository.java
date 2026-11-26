package com.igraded.locker;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LockerFileRepository extends JpaRepository<LockerFile, Long> {

    List<LockerFile> findByOwnerIdOrderByUploadedAtDesc(Long ownerId);

    /** Looks a file up by id AND owner, so one teacher can never reach another teacher's file. */
    Optional<LockerFile> findByIdAndOwnerId(Long id, Long ownerId);

    @Query("select coalesce(sum(f.sizeBytes), 0) from LockerFile f where f.ownerId = :ownerId")
    Long usedBytes(@Param("ownerId") Long ownerId);

    @Query("select distinct f.folder from LockerFile f where f.ownerId = :ownerId")
    List<String> findFolders(@Param("ownerId") Long ownerId);
}
