package com.min.chalkakserver.repository;

import com.min.chalkakserver.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByNameIn(Set<String> names);

    Optional<Tag> findByName(String name);
}
