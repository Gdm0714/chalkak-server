package com.min.chalkakserver.service;

import com.min.chalkakserver.entity.PhotoBooth;
import com.min.chalkakserver.entity.Tag;
import com.min.chalkakserver.exception.PhotoBoothNotFoundException;
import com.min.chalkakserver.repository.PhotoBoothRepository;
import com.min.chalkakserver.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final PhotoBoothRepository photoBoothRepository;

    @Transactional(readOnly = true)
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    @Transactional
    public Tag createTag(String name) {
        return tagRepository.findByName(name).orElseGet(() -> {
            Tag tag = Tag.builder().name(name).build();
            Tag saved = tagRepository.save(tag);
            log.info("Tag created: name={}", name);
            return saved;
        });
    }

    @Transactional
    public void deleteTag(Long id) {
        tagRepository.deleteById(id);
        log.info("Tag deleted: id={}", id);
    }

    @Transactional
    public void setPhotoBoothTags(Long photoBoothId, Set<String> tagNames) {
        PhotoBooth photoBooth = photoBoothRepository.findById(photoBoothId)
                .orElseThrow(() -> new PhotoBoothNotFoundException(photoBoothId));

        Set<Tag> existingTags = new HashSet<>(tagRepository.findByNameIn(tagNames));
        Set<String> existingNames = existingTags.stream().map(Tag::getName).collect(Collectors.toSet());

        for (String name : tagNames) {
            if (!existingNames.contains(name)) {
                Tag newTag = tagRepository.save(Tag.builder().name(name).build());
                existingTags.add(newTag);
            }
        }

        photoBooth.updateTags(existingTags);
        log.info("PhotoBooth tags updated: photoBoothId={}, tags={}", photoBoothId, tagNames);
    }
}
