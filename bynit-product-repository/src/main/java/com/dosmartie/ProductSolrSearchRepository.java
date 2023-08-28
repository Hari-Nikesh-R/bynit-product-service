package com.dosmartie;

import com.dosmartie.document.Product;
import com.dosmartie.document.SolrProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.repository.SolrRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

//@Repository
//public interface ProductSolrSearchRepository extends SolrRepository<SolrProduct, String> {
//    Page<Product> findByBrand(String searchTerm, Pageable pageable);
//
//    List<Product> findByName(String name, Pageable pageable);
//}
