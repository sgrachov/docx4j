/*
 *  Copyright 2007-2008, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */
package org.docx4j.fonts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps font names used in the document to 
 * fonts physically available
 * on the system.
 * 
 * So, a mapper per document.
 * If fonts are added to the document
 * (ie fonts introduced into use)
 * then the mapper should be updated
 * to include a mapping for the
 * new font.
 * 
 * There are 2 implementations:
 * 
 * - IndentityPlusMapper, which is best
 *   where most of the fonts used in the 
 *   document are physically present
 *   on the system
 *   
 * - BestMatchingMapper, useful on
 *   Linux and OSX systems on which
 *   Microsoft fonts have not been 
 *   installed.
 *   
 * Whichever one you use, you can 
 * add/remove mappings programmatically
 * to customise to your needs. 
 * 
 * @author jharrop
 *
 */
public abstract class Mapper {
	
	
	protected static Logger log = LoggerFactory.getLogger(Mapper.class);

	private ConcurrentHashMap<String, PhysicalFont> boldForms = new ConcurrentHashMap<String, PhysicalFont>();
	private ConcurrentHashMap<String, PhysicalFont> italicForms = new ConcurrentHashMap<String, PhysicalFont>();
	private ConcurrentHashMap<String, PhysicalFont> boldItalicForms = new ConcurrentHashMap<String, PhysicalFont>();

	public Mapper() {
		super();
	}
	
	protected final static Map<String, PhysicalFont> fontMappings;
	
	@Deprecated // in order to avoid case sensitivity
	public Map<String, PhysicalFont> getFontMappings() {
		return fontMappings;
	}	
	
	/**
	 * Get a PhysicalFont from FontMappings, 
	 * by case-insensitive name.  (Although Word always
	 * uses Title Case for font names, it is actually
	 * case insensitive; the spec is silent on this.)  
	 * 
	 * @param key
	 * @return
	 */
	public PhysicalFont get(String key) {
		return fontMappings.get(key.toLowerCase());
	}
	/**
	 * Put a PhysicalFont into FontMappings, 
	 * by case-insensitive name.  (Although Word always
	 * uses Title Case for font names, it is actually
	 * case insensitive; the spec is silent on this.)  
	 * 
	 * @param key
	 * @param pf
	 */
	public void put(String key, PhysicalFont pf) {
		fontMappings.put(key.toLowerCase(), pf);
	}
	public int size() {
		return fontMappings.size();
	}
	
	public final static String FONT_FALLBACK = "Times New Roman"; 

	
	static {
		fontMappings = Collections.synchronizedMap(new HashMap<String, PhysicalFont>());
	}
	
	/**
	 * Populate the fontMappings object. We make an entry for each
	 * of the documentFontNames.
	 * 
	 * @param documentFontNames - the fonts used in the document
	 * @param wmlFonts - the content model for the fonts part
	 * @throws Exception
	 */
	public abstract void populateFontMappings(Set<String> documentFontNames, 
			org.docx4j.wml.Fonts wmlFonts ) throws Exception;
	
	
	// For Xalan
	public static String getSubstituteFontXsltExtension(Mapper s, String documentStyleId, String bolditalic, boolean fontFamilyStack) {
		
		return s.getSubstituteFontXsltExtension(documentStyleId, bolditalic, fontFamilyStack);
	}
	
	public String getSubstituteFontXsltExtension(String documentStyleId, 
			String bolditalic, boolean fontFamilyStack) {
		
		log.debug("Trying to insert HTML font-family value for " + documentStyleId);
				
		if (documentStyleId==null) {
			log.error("passed null documentStyleId");
			return "nullInputToExtension";
		}

		
		
		PhysicalFont physicalFont = get((documentStyleId));
		if (physicalFont==null) {

			log.error("No mapping for: " + documentStyleId);
			return Mapper.FONT_FALLBACK;
		} else {

			// iTextFontResolver wants a font family name
			// Until such time as we get this from FOP,
			// use the following heuristic..
			
			String fontFamily = physicalFont.getName();
			
			if (fontFamily.startsWith("Britannic")) { // special case
				return fontFamily;
			}
			if (fontFamily.endsWith(" Demibold" ) ) {
				fontFamily = fontFamily.substring(0, fontFamily.length() - 9);
			}
			if (fontFamily.endsWith(" Oblique" ) ) {
				fontFamily = fontFamily.substring(0, fontFamily.length() - 8);
			}
			if (fontFamily.endsWith(" Italic" ) ) {
				fontFamily = fontFamily.substring(0, fontFamily.length() - 7);
			}
			if (fontFamily.endsWith(" Bold" ) ) {
				fontFamily = fontFamily.substring(0, fontFamily.length() - 5);
			}
			// NB, in that order, it handles " Bold Italic" and "Bold Oblique" as well.
			log.debug("Mapping " + documentStyleId + " to " + physicalFont.getName());
			
			/* On my Windows box, the following are passed
			 * to ITextFontResolver, but still not found in its
			 * _fontFamilies map:
			 * 
			 *      DejaVu Sans ExtraLight
			 *      Lucida Sans Demibold
			 *      Lucida Sans Regular
			 *      Lucida Bright Demibold
			 *      Lucida Sans Demibold Roman
			 *      Lucida Fax Regular
			 *      Lucida Fax Demibold
			 */
			
			return fontFamily;
			
		}
		
//		log.info(documentStyleId + " -> " + physicalFont.getName() );
//		
//		if (fontFamilyStack) {
//			
//			// TODO - if this is an HTML document intended
//			// for viewing in a web browser, we need to add a 
//			// font-family cascade (since the true type font
//			// specified for PDF purposes won't necessarily be
//			// present on web browser's system).
//			
//			// The easiest way to do it might be to just
//			// see whether the substitute font is serif or
//			// not, and add cascade entries accordingly.
//			
//			// If we matched it via FontSubstitutions.xml,
//			// maybe that file contains an HTML match as well?
//			
//			// Either way, this stuff should be worked out in
//			// populateFontMappings, and added to the 
//			// FontMapping objects.
//			
//			return physicalFont.getName();
//		} else {
//			return physicalFont.getName();
//		}
		
		/*
		 * We want to return eg "Times New Roman" 
		 * or "Arial Unicode MS" here, ie _with spaces_, since that is 
		 * what xhtmlrender's org.xhtmlrenderer.pdf.ITextFontResolver sets up.
		 * 
		 * 
		 */
		
	}

	public void registerBoldForm(String fontNameAsInFontTablePart, PhysicalFont pfBold) {
		if (pfBold == null) {
			boldForms.remove(fontNameAsInFontTablePart);
		} else {
			boldForms.put(fontNameAsInFontTablePart, pfBold);
		}
	}

	public void registerItalicForm(String fontNameAsInFontTablePart, PhysicalFont pfItalic) {
		if (pfItalic == null) {
			italicForms.remove(fontNameAsInFontTablePart);
		} else {
			italicForms.put(fontNameAsInFontTablePart, pfItalic);
		}
	}

	public void registerBoldItalicForm(String fontNameAsInFontTablePart, PhysicalFont pfBoldItalic) {
		if (pfBoldItalic == null) {
			boldItalicForms.remove(fontNameAsInFontTablePart);
		} else {
			boldItalicForms.put(fontNameAsInFontTablePart, pfBoldItalic);
		}
	}

	public PhysicalFont getBoldForm(String fontNameAsInFontTablePart, PhysicalFont pf) {
		final PhysicalFont pfBold = boldForms.get(fontNameAsInFontTablePart);
		return (pfBold != null) ? pfBold : PhysicalFonts.getBoldForm(pf);
	}

	public PhysicalFont getItalicForm(String fontNameAsInFontTablePart, PhysicalFont pf) {
		final PhysicalFont pfItalic = italicForms.get(fontNameAsInFontTablePart);
		return (pfItalic != null) ? pfItalic : PhysicalFonts.getItalicForm(pf);
	}

	public PhysicalFont getBoldItalicForm(String fontNameAsInFontTablePart, PhysicalFont pf) {
		final PhysicalFont pfBoldItalic = boldItalicForms.get(fontNameAsInFontTablePart);
		return (pfBoldItalic != null) ? pfBoldItalic : PhysicalFonts.getBoldItalicForm(pf);
	}
}
