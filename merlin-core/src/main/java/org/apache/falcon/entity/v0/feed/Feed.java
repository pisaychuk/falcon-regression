//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.05.28 at 10:55:57 AM PDT 
//


package org.apache.falcon.entity.v0.feed;

import java.util.TimeZone;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.falcon.entity.v0.Entity;
import org.apache.falcon.entity.v0.Frequency;


/**
 * 
 *                 name: A feed should have a unique name and this name is referenced
 *                 by processes as input or output feed.
 *                 tags: a feed specifies an optional list of comma separated tags
 *                 which is used for classification of data sets.
 *                 groups: a feed specifies a list of comma separated groups,
 *                 a group is a logical grouping of feeds and a group is said to be
 *                 available if all the feeds belonging to a group are available.
 *                 The frequency of all
 *                 the feed which belong to the same group
 *                 must be same.
 *                 availabilityFlag: specifies the name of a file which when
 *                 present/created
 *                 in a feeds data directory, the feed is
 *                 termed as available. ex: _SUCCESS, if
 *                 this element is ignored then Falcon would consider the presence of feed's
 *                 data directory as feed availability.
 *                 A feed has a
 *                 frequency and a periodicity which specifies the frequency by which
 *                 this feed is generated. ex: it can be generated every hour, every 5 minutes, daily, weekly etc.
 *                 valid frequency type for a feed are minutes, hours, days, months.
 *             
 * 
 * <p>Java class for feed complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="feed">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="tags" type="{uri:falcon:feed:0.1}KEY_VALUE_PAIR" minOccurs="0"/>
 *         &lt;element name="partitions" type="{uri:falcon:feed:0.1}partitions" minOccurs="0"/>
 *         &lt;element name="groups" type="{uri:falcon:feed:0.1}group-type" minOccurs="0"/>
 *         &lt;element name="availabilityFlag" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="frequency" type="{uri:falcon:feed:0.1}frequency-type"/>
 *         &lt;element name="timezone" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="late-arrival" type="{uri:falcon:feed:0.1}late-arrival" minOccurs="0"/>
 *         &lt;element name="clusters" type="{uri:falcon:feed:0.1}clusters"/>
 *         &lt;choice>
 *           &lt;element name="locations" type="{uri:falcon:feed:0.1}locations"/>
 *           &lt;element name="table" type="{uri:falcon:feed:0.1}catalog-table"/>
 *         &lt;/choice>
 *         &lt;element name="ACL" type="{uri:falcon:feed:0.1}ACL"/>
 *         &lt;element name="schema" type="{uri:falcon:feed:0.1}schema"/>
 *         &lt;element name="properties" type="{uri:falcon:feed:0.1}properties" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{uri:falcon:feed:0.1}IDENTIFIER" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "feed", propOrder = {
    "tags",
    "partitions",
    "groups",
    "availabilityFlag",
    "frequency",
    "timezone",
    "lateArrival",
    "clusters",
    "table",
    "locations",
    "acl",
    "schema",
    "properties"
})
@XmlRootElement(name = "feed")
public class Feed
    extends Entity
{

    protected String tags;
    protected Partitions partitions;
    protected String groups;
    protected String availabilityFlag;
    @XmlElement(required = true, type = String.class)
    @XmlJavaTypeAdapter(Adapter1 .class)
    protected Frequency frequency;
    @XmlElement(type = String.class, defaultValue = "UTC")
    @XmlJavaTypeAdapter(Adapter2 .class)
    protected TimeZone timezone;
    @XmlElement(name = "late-arrival")
    protected LateArrival lateArrival;
    @XmlElement(required = true)
    protected Clusters clusters;
    protected CatalogTable table;
    protected Locations locations;
    @XmlElement(name = "ACL", required = true)
    protected ACL acl;
    @XmlElement(required = true)
    protected Schema schema;
    protected Properties properties;
    @XmlAttribute(name = "name", required = true)
    protected String name;
    @XmlAttribute(name = "description")
    protected String description;

    /**
     * Gets the value of the tags property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTags() {
        return tags;
    }

    /**
     * Sets the value of the tags property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTags(String value) {
        this.tags = value;
    }

    /**
     * Gets the value of the partitions property.
     * 
     * @return
     *     possible object is
     *     {@link Partitions }
     *     
     */
    public Partitions getPartitions() {
        return partitions;
    }

    /**
     * Sets the value of the partitions property.
     * 
     * @param value
     *     allowed object is
     *     {@link Partitions }
     *     
     */
    public void setPartitions(Partitions value) {
        this.partitions = value;
    }

    /**
     * Gets the value of the groups property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGroups() {
        return groups;
    }

    /**
     * Sets the value of the groups property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGroups(String value) {
        this.groups = value;
    }

    /**
     * Gets the value of the availabilityFlag property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAvailabilityFlag() {
        return availabilityFlag;
    }

    /**
     * Sets the value of the availabilityFlag property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAvailabilityFlag(String value) {
        this.availabilityFlag = value;
    }

    /**
     * Gets the value of the frequency property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public Frequency getFrequency() {
        return frequency;
    }

    /**
     * Sets the value of the frequency property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFrequency(Frequency value) {
        this.frequency = value;
    }

    /**
     * Gets the value of the timezone property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public TimeZone getTimezone() {
        return timezone;
    }

    /**
     * Sets the value of the timezone property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTimezone(TimeZone value) {
        this.timezone = value;
    }

    /**
     * Gets the value of the lateArrival property.
     * 
     * @return
     *     possible object is
     *     {@link LateArrival }
     *     
     */
    public LateArrival getLateArrival() {
        return lateArrival;
    }

    /**
     * Sets the value of the lateArrival property.
     * 
     * @param value
     *     allowed object is
     *     {@link LateArrival }
     *     
     */
    public void setLateArrival(LateArrival value) {
        this.lateArrival = value;
    }

    /**
     * Gets the value of the clusters property.
     * 
     * @return
     *     possible object is
     *     {@link Clusters }
     *     
     */
    public Clusters getClusters() {
        return clusters;
    }

    /**
     * Sets the value of the clusters property.
     * 
     * @param value
     *     allowed object is
     *     {@link Clusters }
     *     
     */
    public void setClusters(Clusters value) {
        this.clusters = value;
    }

    /**
     * Gets the value of the table property.
     * 
     * @return
     *     possible object is
     *     {@link CatalogTable }
     *     
     */
    public CatalogTable getTable() {
        return table;
    }

    /**
     * Sets the value of the table property.
     * 
     * @param value
     *     allowed object is
     *     {@link CatalogTable }
     *     
     */
    public void setTable(CatalogTable value) {
        this.table = value;
    }

    /**
     * Gets the value of the locations property.
     * 
     * @return
     *     possible object is
     *     {@link Locations }
     *     
     */
    public Locations getLocations() {
        return locations;
    }

    /**
     * Sets the value of the locations property.
     * 
     * @param value
     *     allowed object is
     *     {@link Locations }
     *     
     */
    public void setLocations(Locations value) {
        this.locations = value;
    }

    /**
     * Gets the value of the acl property.
     * 
     * @return
     *     possible object is
     *     {@link ACL }
     *     
     */
    public ACL getACL() {
        return acl;
    }

    /**
     * Sets the value of the acl property.
     * 
     * @param value
     *     allowed object is
     *     {@link ACL }
     *     
     */
    public void setACL(ACL value) {
        this.acl = value;
    }

    /**
     * Gets the value of the schema property.
     * 
     * @return
     *     possible object is
     *     {@link Schema }
     *     
     */
    public Schema getSchema() {
        return schema;
    }

    /**
     * Sets the value of the schema property.
     * 
     * @param value
     *     allowed object is
     *     {@link Schema }
     *     
     */
    public void setSchema(Schema value) {
        this.schema = value;
    }

    /**
     * Gets the value of the properties property.
     * 
     * @return
     *     possible object is
     *     {@link Properties }
     *     
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the value of the properties property.
     * 
     * @param value
     *     allowed object is
     *     {@link Properties }
     *     
     */
    public void setProperties(Properties value) {
        this.properties = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

}
