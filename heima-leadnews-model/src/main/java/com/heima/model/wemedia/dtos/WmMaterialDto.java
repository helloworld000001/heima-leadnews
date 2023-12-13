package com.heima.model.wemedia.dtos;

import com.heima.model.common.dtos.PageRequestDto;
import lombok.Data;
import org.checkerframework.checker.units.qual.C;

/**
 * @auther 陈彤琳
 * @Description $
 * 2023/12/13 21:55
 */
@Data
public class WmMaterialDto extends PageRequestDto {
    /*
    *  1 查询收藏的   0 未收藏
    * */
    private Short isCollection;
}
