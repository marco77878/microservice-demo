package com.example.uaa.ldap;

import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.DnAttribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;

/**
 * Spring Data LDAP Entry mapping for user objects under ou=users.
 * Maps to LDAP entries with objectClass: inetOrgPerson.
 */
@Entry(base = "ou=users", objectClasses = {"inetOrgPerson", "organizationalPerson", "person", "top"})
public class LdapUserEntry {

    @Id
    private Name dn;

    @Attribute(name = "cn")
    @DnAttribute(value = "cn")
    private String cn;

    @Attribute(name = "sn")
    private String sn;

    @Attribute(name = "uid")
    private String uid;

    @Attribute(name = "userPassword")
    private String userPassword;

    public LdapUserEntry() {
    }

    public Name getDn() {
        return dn;
    }

    public void setDn(Name dn) {
        this.dn = dn;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    @Override
    public String toString() {
        return "LdapUserEntry{cn='" + cn + "', uid='" + uid + "', sn='" + sn + "'}";
    }
}
