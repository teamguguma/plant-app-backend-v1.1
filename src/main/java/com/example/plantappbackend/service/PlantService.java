package com.example.plantappbackend.service;

import com.example.plantappbackend.DTO.PlantDTOWithRecentStatus;
import com.example.plantappbackend.DTO.PlantStatusDTO;
import com.example.plantappbackend.model.Plant;
import com.example.plantappbackend.model.PlantStatus;
import com.example.plantappbackend.repository.PlantRepository;
import com.example.plantappbackend.repository.PlantStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlantService {

    private final PlantRepository plantRepository;
    private final PlantStatusRepository plantStatusRepository;

    @Autowired
    public PlantService(PlantRepository plantRepository, PlantStatusRepository plantStatusRepository) {
        this.plantRepository = plantRepository;
        this.plantStatusRepository = plantStatusRepository;
    }

    // 특정 Plant ID로 조회
    public Plant getPlantById(Long id) {
        return plantRepository.findById(id).orElse(null);
    }

    // 식물 데이터 저장
    public Plant savePlant(Plant plant) {
        return plantRepository.save(plant);
    }

    // 특정 사용자의 닉네임 중복 확인
    public boolean isDuplicateNickname(String plantNickname, String userUuid) {
        return plantRepository.findAllByUserUuid(userUuid)
                .stream()
                .anyMatch(plant -> plant.getPlantNickname().equals(plantNickname));
    }

    // 식물 목록 최신 상태 포함 DTO 반환
    public List<PlantDTOWithRecentStatus> getPlantsByUserWithRecentStatus(String userUuid) {
        return plantRepository.findAllByUserUuid(userUuid)
                .stream()
                .map(this::convertToPlantDTOWithRecentStatus)
                .collect(Collectors.toList());
    }

    private PlantDTOWithRecentStatus convertToPlantDTOWithRecentStatus(Plant plant) {
        PlantDTOWithRecentStatus plantDTO = new PlantDTOWithRecentStatus();
        plantDTO.setId(plant.getId());
        plantDTO.setPlantNickname(plant.getPlantNickname());
        plantDTO.setPlantName(plant.getPlantName());
        plantDTO.setCheckDateInterval(plant.getCheckDateInterval());
        plantDTO.setCreatedAt(plant.getCreatedAt());

        // 가장 최근 상태 설정
        PlantStatus latestStatus = plantStatusRepository.findTopByPlantIdOrderByCreatedAtDesc(plant.getId());
        if (latestStatus != null) {
            PlantStatusDTO recentStatusDTO = convertToPlantStatusDTO(latestStatus);
            plantDTO.setRecentStatus(recentStatusDTO);
        }

        return plantDTO;
    }

    // PlantStatus를 DTO로 변환
    private PlantStatusDTO convertToPlantStatusDTO(PlantStatus plantStatus) {
        PlantStatusDTO statusDTO = new PlantStatusDTO();
        statusDTO.setStatusId(plantStatus.getId());
        statusDTO.setStatus(plantStatus.getStatus());
        statusDTO.setRemedy(plantStatus.getRemedy());
        statusDTO.setImageUrl(plantStatus.getImageUrl());
        statusDTO.setCreatedAt(plantStatus.getCreatedAt());
        return statusDTO;
    }

    // 특정 Plant ID로 최신 상태 포함 식물 조회
    public PlantDTOWithRecentStatus getPlantWithRecentStatusById(Long plantId) {
        // Plant 엔터티 조회
        Plant plant = plantRepository.findById(plantId).orElse(null);
        if (plant == null) {
            return null;
        }

        // Plant 엔터티를 DTO로 변환
        PlantDTOWithRecentStatus plantDTO = new PlantDTOWithRecentStatus();
        plantDTO.setId(plant.getId());
        plantDTO.setPlantNickname(plant.getPlantNickname());
        plantDTO.setPlantName(plant.getPlantName());
        plantDTO.setCheckDateInterval(plant.getCheckDateInterval());
        plantDTO.setCreatedAt(plant.getCreatedAt());

        // 가장 최근 상태 조회 및 DTO 변환
        PlantStatus latestStatus = plantStatusRepository.findTopByPlantIdOrderByCreatedAtDesc(plantId);
        if (latestStatus != null) {
            PlantStatusDTO recentStatusDTO = convertToPlantStatusDTO(latestStatus);
            plantDTO.setRecentStatus(recentStatusDTO);
        }

        return plantDTO;
    }

    // 특정 식물 삭제
    public boolean deletePlantById(Long plantId) {
        if (!plantRepository.existsById(plantId)) {
            return false; // 식물이 존재하지 않음
        }
        plantRepository.deleteById(plantId); // 식물 삭제
        return true;
    }

    public boolean updatePlantById(Long plantId, String nickname, String name, Integer checkDateInterval) {
        // 해당 식물이 존재하는지 확인
        Plant plant = plantRepository.findById(plantId).orElse(null);
        if (plant == null) {
            return false; // 식물이 존재하지 않음
        }

        // 닉네임 중복 검사
        if (nickname != null) {
            boolean isDuplicate = plantRepository.findAllByUserUuid(plant.getUserUuid())
                    .stream()
                    .anyMatch(existingPlant -> !existingPlant.getId().equals(plantId) && existingPlant.getPlantNickname().equals(nickname));
            if (isDuplicate) {
                throw new IllegalArgumentException("닉네임이 중복됩니다."); // 중복된 닉네임인 경우 예외 발생
            }
            plant.setPlantNickname(nickname);
        }

        // null이 아닌 필드만 수정
        if (name != null) {
            plant.setPlantName(name);
        }
        if (checkDateInterval != null) {
            plant.setCheckDateInterval(checkDateInterval);
        }

        // 저장
        plantRepository.save(plant);
        return true;
    }

}
