package com.gifticon.repository;

import com.gifticon.domain.Gifticon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GifticonRepository extends JpaRepository<Gifticon, Long> {
    Gifticon findByShareCode(String shareCode);
} 