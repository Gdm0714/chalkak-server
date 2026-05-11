package com.min.chalkakserver.dto;

import com.min.chalkakserver.entity.PhotoBoothImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoBoothImageDto {

    private Long id;
    private String imageUrl;
    private String imageType;
    private Integer displayOrder;

    public static PhotoBoothImageDto from(PhotoBoothImage image) {
        return PhotoBoothImageDto.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .imageType(image.getImageType().name())
                .displayOrder(image.getDisplayOrder())
                .build();
    }
}
