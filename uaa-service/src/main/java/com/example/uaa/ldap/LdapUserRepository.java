package com.example.uaa.ldap;

import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data LDAP Repository for LdapUserEntry.
 * Provides automatic query derivation for LDAP searches.
 */
@Repository
public interface LdapUserRepository extends LdapRepository<LdapUserEntry> {

    /**
     * Find a user entry by Common Name (cn) attribute.
     * Spring Data LDAP auto-generates the LDAP filter: (cn={username})
     */
    Optional<LdapUserEntry> findByCn(String cn);

    /**
     * Find a user entry by uid attribute.
     * Spring Data LDAP auto-generates the LDAP filter: (uid={uid})
     */
    Optional<LdapUserEntry> findByUid(String uid);
}
