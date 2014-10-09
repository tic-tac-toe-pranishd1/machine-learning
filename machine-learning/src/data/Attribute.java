package data;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import bimap.BiMap;

/**
 * Represents a quantity describing an instance.  Every attribute has a domain
 * of possible values.  An attribute, value pair forms a feature for a given
 * instance. 
 *
 */
public class Attribute 
{
    public enum Type {NOMINAL, CONTINUOUS};
	
	/**
	 * Attribute name
	 */
	private final String name;
	
	/**
	 * This maps a name of a nominal value for this attribute to a unique
	 * nominal value integer ID
	 */
	private final BiMap<String, Integer> nominalValueMap;

	private final Type type;
	
	/**
	 * This Attribute's unique integer ID
	 */
	private final int id;
	
	/**
	 * Constructor
	 * 
	 * @param name - name of the attribute
	 * @param id - integer id of the attribute 
	 * @param type - {nominal, continuous}
	 */
	public Attribute(String name, int id, Type type, String[] nominalValues)
	{
		this.name = name;
		this.id = id;
		this.type = type;
		
		switch(type)
		{
		case NOMINAL:
		    this.nominalValueMap = new BiMap<>();
		    
            int count = 0;
            for (String value : nominalValues)
            {     
                nominalValueMap.put(value, count++);
            }
            break;
		default:
	          nominalValueMap = null; 
	          break;
		}	
	}
	
	
	/**
	 * Get this attribute's id.  In an ARFF file, this ID corresponds
	 * to its index in the ARFF header.
	 * 
	 * @return Integer - the attribute's ID
	 */
	public Integer getId()
	{
		return id;
	}
	
	/**
	 * Get the attribute's type (i.e. continuous or nominal)
	 * 
	 * @return The attribute's type
	 */
	public Type getType()
	{
		return type;
	}
	
	/**
	 * @return the attribute's name
	 */
	public String getName()
	{
	    return name;
	}
	
	/**
	 * Determine whether a particular nominal value ID is a valid nominal value
	 * ID for this attribute
	 * 
	 * @param nomValueId the nominal value ID
	 * @return true if this nominal value ID is valid for this attribute
	 */
	public Boolean isValidNominalValueId(Integer nomValueId)
	{
	    return nominalValueMap.values().contains(nomValueId);
	}
	
	/**
	 * Get the attribute's nominal value map. The map's keys correspond
	 * to the name of the value and it's values correspond to that value's 
	 * integer id.  If this attribute is continuous, then this method will 
	 * return null.
	 *  
	 * @return the nominal value map
	 */
	public Map<String, Integer> getNominalValueMap()
	{
		return nominalValueMap;
	}
	
	/**
	 * If this attribute is a nominal attribute, this method will return the 
	 * name of a nominal value for this attribute given the nominal value ID of 
	 * that value
	 * 
	 * @param attrValueId - The Id of the attribute's value
	 * @return the name of the attribute's value with this ID or
	 * 		   null if this is an invalid attribute value or the attribute is
	 * 		   continuous
	 */
	public String getNominalValueName(Integer attrValueId)
	{
		
		if (this.type == Attribute.Type.CONTINUOUS)
		{
		    /*
			System.err.println("Error retrieving nominal value name for" +
					" the ID " + attrValueId +
					". Trying to retrieve a nominal value from a continuous " +
					"attribute, " + 
					this.name + ".");
			return nominalValueName;*/
		}
		
		if  (!nominalValueMap.containsValue(attrValueId))
		{
		    // TODO: Throw a different type of exception
		    throw new RuntimeException("Error retrieving nominal value name " +
                    "for the ID " + attrValueId +
                    ".  This value ID is not a possible value for the " +
                    "attribute " + this.name + ".");
 
		}
		
		return nominalValueMap.getKey(attrValueId);
	}
	
	/**
	 * If this attribute is a nominal attribute, this method will return an 
	 * integer corresponding to the ID of this attribute's possible value.
	 * 
	 * @param attrValueName - The name of the attribute's value
	 * @return Integer - The ID of the attributes value.
	 * 		   null    - If this is an invalid attribute value or the attribute 
	 * 					 is continuous
	 */
	public Integer getNominalValueId(String attrValueName)
	{
		Integer valueId = null;
		
		if (this.type == Attribute.Type.CONTINUOUS)
		{
			System.err.println("Error retrieving nominal value Id for the " +
					"value " + attrValueName +
					". Trying to retrieve a nominal value from a continuous " +
					"attribute, " + this.name + ".");
			return valueId;
		}
		
		if  (!nominalValueMap.containsKey(attrValueName))
		{
			System.err.println("Error retrieving nominal value Id for the " +
					"value " + attrValueName + ".  This value name is not a " +
					"possible value for the attribute " + this.name + ".");
			return valueId;
		}
		
		return nominalValueMap.getValue(attrValueName);
	}
}
