package com.teraha.products.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;


import com.teraha.commons.entities.Product;
import com.teraha.commons.entities.Category;
import com.teraha.commons.entities.Supplier;
import com.teraha.commons.dtos.ProductDTO;
import com.teraha.commons.dtos.SupplierDTO;
import com.teraha.commons.dtos.CategoryDTO;

@Mapper(componentModel = "spring")
public interface ProductMapper{

	@BeanMapping(builder = @Builder(disableBuilder = true))
	ProductDTO toDto(Product product);

	Product toEntity(ProductDTO productDTO);

	CategoryDTO toDto(Category category);

	@Mapping(target="products", ignore=true)
	Category toEntity(CategoryDTO categoryDto);

	SupplierDTO toDto(Supplier supplier);

	@Mapping(target="products", ignore=true)
	Supplier toEntity(SupplierDTO supplierDto);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntity(
			ProductDTO productDTO,
			@MappingTarget Product product );
}
