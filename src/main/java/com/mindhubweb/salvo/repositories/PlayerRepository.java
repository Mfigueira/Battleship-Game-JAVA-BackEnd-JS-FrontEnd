package com.mindhubweb.salvo.repositories;

import com.mindhubweb.salvo.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface PlayerRepository extends JpaRepository<Player, Long> {

    Player findByUserName(@Param("name") String name);
    Player findByEmail(@Param("email") String email);

}