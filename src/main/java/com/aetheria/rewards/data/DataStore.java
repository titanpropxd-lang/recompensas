package com.aetheria.rewards.data;

import java.util.UUID;

/** Almacen de datos de jugador (el contenido es el YAML serializado de PlayerData). */
public interface DataStore {
    /** @return el YAML guardado o null si el jugador no tiene datos. Lanza excepcion si el almacen falla (nunca devuelve null por error). */
    String load(UUID id) throws Exception;
    void save(UUID id, String yaml) throws Exception;
    String name();
}
