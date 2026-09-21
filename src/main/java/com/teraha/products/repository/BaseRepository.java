package com.teraha.products.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.NoRepositoryBean;
import com.teraha.commons.entities.BaseEntity;

@NoRepositoryBean
public interface BaseRepository<E extends BaseEntity> extends PagingAndSortingRepository<E, Long> {}
