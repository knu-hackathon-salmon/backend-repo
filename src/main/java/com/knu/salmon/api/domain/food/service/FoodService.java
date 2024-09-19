package com.knu.salmon.api.domain.food.service;

import com.knu.salmon.api.domain.Image.entity.FoodImage;
import com.knu.salmon.api.domain.Image.repository.FoodImageRepository;
import com.knu.salmon.api.domain.Image.service.FoodImageService;
import com.knu.salmon.api.domain.food.dto.request.CreateFoodDto;
import com.knu.salmon.api.domain.food.dto.request.UpdateFoodDto;
import com.knu.salmon.api.domain.food.dto.response.FoodDetailResponseDto;
import com.knu.salmon.api.domain.food.dto.response.FoodOverviewResponseDto;
import com.knu.salmon.api.domain.food.entity.Food;
import com.knu.salmon.api.domain.food.repository.FoodRepository;
import com.knu.salmon.api.domain.member.entity.Member;
import com.knu.salmon.api.domain.member.entity.PrincipalDetails;
import com.knu.salmon.api.domain.member.repository.MemberRepository;
import com.knu.salmon.api.domain.seller.entity.Shop;
import com.knu.salmon.api.domain.seller.repository.ShopRepository;
import com.knu.salmon.api.global.error.custom.FoodException;
import com.knu.salmon.api.global.error.custom.MemberException;
import com.knu.salmon.api.global.error.errorcode.MemberErrorCode;
import com.knu.salmon.api.global.error.errorcode.custom.FoodErrorCode;
import com.knu.salmon.api.global.spec.response.ApiBasicResponse;
import com.knu.salmon.api.global.spec.response.ApiDataResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.sql.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FoodService {
    private final FoodRepository foodRepository;
    private final MemberRepository memberRepository;
    private final FoodImageService foodImageService;
    private final FoodImageRepository foodImageRepository;
    private final ShopRepository shopRepository;

    public ApiDataResponse<FoodDetailResponseDto> createFood(MultipartFile[] files, CreateFoodDto createFoodDto, PrincipalDetails principalDetails){
        Member member = memberRepository.findByEmail(principalDetails.getEmail())
                .orElseThrow(() -> new MemberException(MemberErrorCode.No_EXIST_EMAIL_MEMBER_EXCEPTION));
        Shop shop = shopRepository.findByMemberId(member.getId());

        Food food = Food.builder()
                .title(createFoodDto.getTitle())
                .name(createFoodDto.getName())
                .foodCategory(createFoodDto.getFoodCategory())
                .stock(createFoodDto.getStock())
                .price(createFoodDto.getPrice())
                .content(createFoodDto.getContent())
                .expiration(createFoodDto.getExpiration())
                .shop(shop)
                .trading(true)
                .build();
        foodRepository.save(food);

        shop.getFoodList().add(food);
        shopRepository.save(shop);
        foodImageService.uploadToBoardImages(files, food);

        return ApiDataResponse.<FoodDetailResponseDto>builder()
                .status(true)
                .code(200)
                .message("글 생성을 성공! 추가 된 데이터는 다음과 같습니다")
                .data(FoodDetailResponseDto.fromFood(food))
                .build();
    }

    public ApiDataResponse<FoodDetailResponseDto> getFoodDetail(Long foodId){
        Food food =  foodRepository.findById(foodId)
                .orElseThrow(() -> new FoodException(FoodErrorCode.NO_EXIST_FOOD_EXCEPTION));

        return ApiDataResponse.<FoodDetailResponseDto>builder()
                .status(true)
                .code(200)
                .message("글 불러오기 성공! 불러 온 데이터는 다음과 같습니다")
                .data(FoodDetailResponseDto.fromFood(food))
                .build();
    }

    public ApiDataResponse<List<FoodOverviewResponseDto>> getFoodOverview(){
        List<Food> foodList = foodRepository.findAll();
        List<FoodOverviewResponseDto> responseDtoList = foodList.stream()
                .map(FoodOverviewResponseDto::fromFood).toList();


        return ApiDataResponse.<List<FoodOverviewResponseDto>>builder()
                .status(true)
                .code(200)
                .message("음식 리스트 반환 성공! 불러 온 데이터는 다음과 같습니다")
                .data(responseDtoList)
                .build();
    }

    public ApiDataResponse<FoodDetailResponseDto> updateFood(UpdateFoodDto updateFoodDto, MultipartFile[] newImageList, PrincipalDetails principalDetails, Long foodId){
        Member member = memberRepository.findByEmail(principalDetails.getEmail())
                .orElseThrow(() -> new MemberException(MemberErrorCode.No_EXIST_EMAIL_MEMBER_EXCEPTION));

        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new FoodException(FoodErrorCode.NO_EXIST_FOOD_EXCEPTION));

        if(!food.getShop().getMember().equals(member)){
            throw new MemberException(MemberErrorCode.NO_OWNER_EXCEPTION);
        }

        //////////////////////

        return ApiDataResponse.<FoodDetailResponseDto>builder()
                .status(true)
                .code(200)
                .message("음식 정보 업데이트 성공! 업데이트 한 이후 데이터는 다음과 같습니다")
                .data(FoodDetailResponseDto.fromFood(food))
                .build();
    }

    public ApiBasicResponse deleteFood(PrincipalDetails principalDetails, Long foodId) {
        Member member = memberRepository.findByEmail(principalDetails.getEmail())
                .orElseThrow(() -> new MemberException(MemberErrorCode.No_EXIST_EMAIL_MEMBER_EXCEPTION));

        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new FoodException(FoodErrorCode.NO_EXIST_FOOD_EXCEPTION));

        if(!food.getShop().getMember().equals(member)){
            throw new MemberException(MemberErrorCode.NO_OWNER_EXCEPTION);
        }

        foodRepository.delete(food);

        return ApiBasicResponse.builder()
                .status(true)
                .code(200)
                .message("음식 삭제 성공")
                .build();
    }


}
