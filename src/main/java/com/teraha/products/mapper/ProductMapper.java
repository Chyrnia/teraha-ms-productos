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

	@Mapping(target = "categoryIds", source = "categories")
	@Mapping(target = "supplierIds", source = "suppliers")
	@BeanMapping(builder = @Builder(disableBuilder = true))
	ProductDTO toDto(Product product);

	@Mapping(target="categories", ignore=true)
	@Mapping(target="suppliers", ignore=true)
	Product toEntity(ProductDTO productDTO);

	CategoryDTO toDto(Category category);

	@Mapping(target="products", ignore=true)
	Category toEntity(CategoryDTO categoryDto);

	SupplierDTO toDto(Supplier supplier);

	@Mapping(target="products", ignore=true)
	Supplier toEntity(SupplierDTO supplierDto);

	@Mapping(target="categories", ignore=true)
	@Mapping(target="suppliers", ignore=true)
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntity(
			ProductDTO productDTO,
			@MappingTarget Product product );

	default Long map(Category category){
		return category == null ? null : category.getId();
	}

	default Long map(Supplier supplier){
		return supplier == null ? null : supplier.getId();
	}

}
