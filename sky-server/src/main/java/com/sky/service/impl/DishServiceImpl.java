package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
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
import org.springframework.util.StringUtils;

import java.io.ObjectInputFilter;
import java.util.ArrayList;
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
    @Autowired
    private SetmealMapper setmealMapper;

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
        try {
            //删除菜品数据
            List<String> urls = dishMapper.selectByIds(ids);
            if (urls.isEmpty()){
                throw new DeletionNotAllowedException("菜品图片 "+ids+" 不存在，无法删除");
            }
            dishMapper.deleteByIds(ids);
            for (String imageUrl : urls) {
                while (imageUrl.endsWith("/")){
                 imageUrl=imageUrl.substring(0,imageUrl.length()-1);
              }
               if(StringUtils.hasText(imageUrl)) {
                   String objectName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                   if(objectName == null || objectName.isEmpty()){
                       throw new DeletionNotAllowedException(objectName+"不存在");
                   }
                    String bucketName="dish-thumbnail";
                   minioUtil.delete(bucketName,objectName);
               }
            }
            dishFlavorMapper.deleteByDishIds(ids);
        } catch (DeletionNotAllowedException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * 根据id查询对应的菜品和口味数据
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品数据
        Dish dish = dishMapper.getById(id);
        //根据菜品id查询口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //将查询到的数据封装VO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 修改菜品和口味信息
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        List<DishFlavor> flavors = dishDTO.getFlavors();
        Long dishId=dishDTO.getId();
        if (flavors!=null&& !flavors.isEmpty()){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            dishFlavorMapper.insertBatch(flavors);
        }



    }
    /**
     * 修改菜品起售和停售
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish=Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);
        if(status==StatusConstant.DISABLE){
            List<Long>dishIds=new ArrayList<>();
            dishIds.add(id);
            //select setmeal_id from setmeal_dish where dish_id in ()
            List<Long> setmealIds=setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if(setmealIds!=null&&!setmealIds.isEmpty()){
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }

        }
    }

    @Override
    public List<Dish> list(Long categoryId) {
        Dish dish=Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }
}
