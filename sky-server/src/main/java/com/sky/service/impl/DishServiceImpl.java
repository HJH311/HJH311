package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.utils.MinioUtil;
import com.sky.vo.DishVO;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ObjectInputFilter;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    /**
     * 新增菜品和口味表
     * @param dishDTO
     */
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private MinioUtil minioUtil;
    //多个数据表的操作，需要事务
    @Transactional
    @Override
    public void saveWithFlavor(DishDTO dishDTO) {
        //向菜品表插入一条数据
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        //获取插入之后的id
        Long dishId = dish.getId();
        //向口味表插入N条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() >0){
            //每个口味初始化dishId
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(flavors);
        }


    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageOuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page =dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult() );
    }

    /**
     * 菜品的批量删除
     * @param ids
     */
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        //判断起售
        for (Long id : ids) {
            Dish dish=dishMapper.getById(id);
            if(dish==null){
                throw new DeletionNotAllowedException("菜品ID " + id + " 不存在，无法删除");
            }
            if(Objects.equals(dish.getStatus(), StatusConstant.ENABLE)){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断菜品关联
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size()>0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品数据
        for (Long id : ids) {
            //删除图片
            String imageUrl=dishMapper.deleteImageById(id);
            while (imageUrl.endsWith("/")){
                imageUrl=imageUrl.substring(0,imageUrl.length()-1);
            }
            if(imageUrl !="" && !imageUrl.isEmpty()) {
                String objectName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                if(objectName == null || objectName.isEmpty()){
                    throw new DeletionNotAllowedException(objectName+"不存在");
                }
                String bucketName="dish-thumbnail";
                minioUtil.delete(bucketName,objectName);
                dishMapper.deleteById(id);
                //删除关联的口味数据
                dishFlavorMapper.deleteByDishId(id);
            }

        }


    }
}
