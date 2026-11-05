package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.CommunityCreateDTO;
import com.sky.dto.StoreProductPublishDTO;
import com.sky.entity.Community;
import com.sky.entity.CommunityMember;
import com.sky.entity.StoreCategory;
import com.sky.entity.StoreProduct;
import com.sky.result.Result;
import com.sky.service.CommunityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 社群接口（阶段一：私域交易工具）
 */
@RestController
@RequestMapping("/user/community")
@Api(tags = "社群接口")
@Slf4j
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    /** 创建社群（成为团长） */
    @PostMapping("/create")
    @ApiOperation("创建社群")
    public Result<Community> create(@RequestBody CommunityCreateDTO dto) {
        Community community = communityService.create(dto, BaseContext.getCurrentId());
        return Result.success(community);
    }

    /** 通过邀请码加入社群 */
    @PostMapping("/join")
    @ApiOperation("通过邀请码加入社群")
    public Result<Community> join(@RequestParam String inviteCode) {
        Community community = communityService.join(inviteCode, BaseContext.getCurrentId());
        return Result.success(community);
    }

    /** 我的社群列表 */
    @GetMapping("/list")
    @ApiOperation("我的社群列表")
    public Result<List<Community>> list() {
        return Result.success(communityService.myList(BaseContext.getCurrentId()));
    }

    /** 首页商品流：我加入的社群里团长发布的商品 */
    @GetMapping("/feed")
    @ApiOperation("首页商品流")
    public Result<List<StoreProduct>> feed() {
        return Result.success(communityService.feed(BaseContext.getCurrentId()));
    }

    /** 社群成员列表（我的团员） */
    @GetMapping("/members")
    @ApiOperation("社群成员列表")
    public Result<List<CommunityMember>> members(@RequestParam Long communityId) {
        return Result.success(communityService.members(communityId, BaseContext.getCurrentId()));
    }

    /** 团长发布商品到社群 */
    @PostMapping("/product")
    @ApiOperation("团长发布商品")
    public Result<StoreProduct> publishProduct(@RequestBody StoreProductPublishDTO dto) {
        return Result.success(communityService.publishProduct(dto, BaseContext.getCurrentId()));
    }

    /** 商品详情 */
    @GetMapping("/product/detail")
    @ApiOperation("商品详情")
    public Result<StoreProduct> productDetail(@RequestParam Long id) {
        return Result.success(communityService.productDetail(id));
    }

    /** 商品搜索（限定我加入的社群） */
    @GetMapping("/product/search")
    @ApiOperation("商品搜索")
    public Result<List<StoreProduct>> search(@RequestParam String keyword) {
        return Result.success(communityService.searchProduct(keyword, BaseContext.getCurrentId()));
    }

    /** 我的店铺商品列表 */
    @GetMapping("/product/owner-list")
    @ApiOperation("我的店铺商品")
    public Result<List<StoreProduct>> ownerList() {
        return Result.success(communityService.ownerProducts(BaseContext.getCurrentId()));
    }

    /** 修改商品（信息/上下架；仅团长） */
    @PutMapping("/product")
    @ApiOperation("修改商品")
    public Result<StoreProduct> updateProduct(@RequestBody StoreProductPublishDTO dto) {
        return Result.success(communityService.updateProduct(dto, BaseContext.getCurrentId()));
    }

    /** 社群商品分类 */
    @GetMapping("/categories")
    @ApiOperation("社群商品分类")
    public Result<List<StoreCategory>> categories(@RequestParam Long communityId) {
        return Result.success(communityService.categories(communityId));
    }
}
