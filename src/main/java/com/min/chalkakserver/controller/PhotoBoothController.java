package com.min.chalkakserver.controller;

import com.min.chalkakserver.dto.PhotoBoothResponseDto;
import com.min.chalkakserver.service.PhotoBoothService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/photo-booths")
@CrossOrigin(origins = "*")
public class PhotoBoothController {
    
    @Autowired
    private PhotoBoothService photoBoothService;
    
    @GetMapping
    public ResponseEntity<List<PhotoBoothResponseDto>> getAllPhotoBooths() {
        List<PhotoBoothResponseDto> photoBooths = photoBoothService.getAllPhotoBooths();
        return ResponseEntity.ok(photoBooths);
    }
    
    @GetMapping("/nearby")
    public ResponseEntity<List<PhotoBoothResponseDto>> getNearbyPhotoBooths(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "3.0") double radius) {
        List<PhotoBoothResponseDto> nearbyBooths = photoBoothService.getNearbyPhotoBooths(latitude, longitude, radius);
        return ResponseEntity.ok(nearbyBooths);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PhotoBoothResponseDto> getPhotoBoothById(@PathVariable Long id) {
        PhotoBoothResponseDto photoBooth = photoBoothService.getPhotoBoothById(id);
        return ResponseEntity.ok(photoBooth);
    }
}
