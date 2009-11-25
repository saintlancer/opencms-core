/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/jsp/CmsJspNavBuilder.java,v $
 * Date   : $Date: 2009/11/25 15:24:15 $
 * Version: $Revision: 1.3 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) 2002 - 2009 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.jsp;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;

/**
 * Bean to provide a convenient way to build navigation structures based on the
 * <code>{@link org.opencms.jsp.CmsJspNavElement}</code>.<p>
 *
 * Use this together with the <code>{@link org.opencms.jsp.CmsJspActionElement}</code>
 * to obtain navigation information based on the current users permissions.
 * For example, use <code>{@link #getNavigationForFolder(String)}</code> and pass the 
 * value of the current OpenCms user context uri obtained 
 * from <code>{@link org.opencms.file.CmsRequestContext#getUri()}</code> as argument to obtain a list
 * of all items in the navigation of the current folder. Then use a simple scriptlet to 
 * iterate over these items and create a HTML navigation.<p>
 *
 * @author  Alexander Kandzior 
 * 
 * @version $Revision: 1.3 $ 
 * 
 * @since 6.0.0 
 * 
 * @see org.opencms.jsp.CmsJspNavElement
 */
public class CmsJspNavBuilder {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsJspNavBuilder.class);

    /** The current CMS context. */
    private CmsObject m_cms;

    /** The current request URI. */
    private String m_requestUri;

    /** The current request folder. */
    private String m_requestUriFolder;

    /**
     * Empty constructor, so that this bean can be initialized from a JSP.<p> 
     */
    public CmsJspNavBuilder() {

        // empty
    }

    /**
     * Default constructor.<p>
     * 
     * @param cms context provider for the current request
     */
    public CmsJspNavBuilder(CmsObject cms) {

        init(cms);
    }

    /**
     * Returns the full name (including VFS path) of the default file for this navigation element 
     * or <code>null</code> if the navigation element is not a folder.<p>
     * 
     * The default file of a folder is determined by the value of the property 
     * <code>default-file</code> or the system wide property setting.<p>
     * 
     * @param cms the CMS object
     * @param folder full name of the folder
     * 
     * @return the name of the default file
     */
    public static String getDefaultFile(CmsObject cms, String folder) {

        if (folder.endsWith("/")) {
            List<String> defaultFolders = new ArrayList<String>();
            try {
                CmsProperty p = cms.readPropertyObject(folder, CmsPropertyDefinition.PROPERTY_DEFAULT_FILE, false);
                if (!p.isNullProperty()) {
                    defaultFolders.add(p.getValue());
                }
            } catch (CmsException e) {
                // should never happen
                LOG.error(e.getLocalizedMessage(), e);
            }

            defaultFolders.addAll(OpenCms.getDefaultFiles());

            for (String defaultName : defaultFolders) {
                if (cms.existsResource(folder + defaultName)) {
                    return folder + defaultName;
                }
            }
            return folder;
        }
        return null;
    }

    /**
     * Collect all navigation elements from the files in the given folder,
     * navigation elements are of class {@link CmsJspNavElement}.<p>
     *
     * @param cms context provider for the current request
     * @param folder the selected folder
     * 
     * @return a sorted (ascending to navigation position) list of navigation elements
     */
    public static List<CmsJspNavElement> getNavigationForFolder(CmsObject cms, String folder) {

        folder = CmsResource.getFolderPath(folder);
        List<CmsJspNavElement> result = new ArrayList<CmsJspNavElement>();

        List<CmsResource> resources;
        try {
            resources = cms.getResourcesInFolder(folder, CmsResourceFilter.DEFAULT);
        } catch (Exception e) {
            // should never happen
            LOG.error(e.getLocalizedMessage(), e);
            return Collections.<CmsJspNavElement> emptyList();
        }

        for (CmsResource r : resources) {
            CmsJspNavElement element = getNavigationForResource(cms, cms.getSitePath(r));
            if ((element != null) && element.isInNavigation()) {
                result.add(element);
            }
        }
        Collections.sort(result);
        return result;
    }

    /** 
     * Build a navigation for the folder that is either minus levels up 
     * from the given folder, or that is plus levels down from the 
     * root folder towards the given folder.<p> 
     * 
     * If level is set to zero the root folder is used by convention.<p>
     * 
     * @param cms context provider for the current request
     * @param folder the selected folder
     * @param level if negative, walk this many levels up, if positive, walk this many 
     *              levels down from root folder 
     *              
     * @return a sorted (ascending to navigation position) list of navigation elements
     */
    public static List<CmsJspNavElement> getNavigationForFolder(CmsObject cms, String folder, int level) {

        folder = CmsResource.getFolderPath(folder);
        // If level is one just use root folder
        if (level == 0) {
            return getNavigationForFolder(cms, "/");
        }
        String navfolder = CmsResource.getPathPart(folder, level);
        // If navigation folder found use it to build navigation
        if (navfolder != null) {
            return getNavigationForFolder(cms, navfolder);
        }
        // Nothing found, return empty list
        return Collections.<CmsJspNavElement> emptyList();
    }

    /**
     * Returns a CmsJspNavElement for the named resource.<p>
     * 
     * @param cms context provider for the current request
     * @param resource the resource name to get the navigation information for, 
     *              must be a full path name, e.g. "/docs/index.html"
     *              
     * @return a navigation element for the given resource
     */
    public static CmsJspNavElement getNavigationForResource(CmsObject cms, String resource) {

        List<CmsProperty> properties;
        try {
            properties = cms.readPropertyObjects(resource, false);
        } catch (Exception e) {
            // should never happen
            LOG.error(e.getLocalizedMessage(), e);
            return null;
        }
        int level = CmsResource.getPathLevel(resource);
        if (resource.endsWith("/")) {
            level--;
        }
        return new CmsJspNavElement(resource, CmsProperty.toMap(properties), level);
    }

    /**
     * Builds a tree navigation for the folders between the provided start and end level.<p>
     * 
     * A tree navigation includes all navigation elements that are required to display a tree structure.
     * However, the data structure is a simple list.
     * Each of the navigation elements in the list has the {@link CmsJspNavElement#getNavTreeLevel()} set
     * to the level it belongs to. Use this information to distinguish between the navigation levels.<p>
     * 
     * @param cms context provider for the current request
     * @param folder the selected folder
     * @param startlevel the start level
     * @param endlevel the end level
     * 
     * @return a sorted list of navigation elements with the navigation tree level property set 
     */
    public static List<CmsJspNavElement> getNavigationTreeForFolder(
        CmsObject cms,
        String folder,
        int startlevel,
        int endlevel) {

        folder = CmsResource.getFolderPath(folder);
        // Make sure start and end level make sense
        if (endlevel < startlevel) {
            return Collections.<CmsJspNavElement> emptyList();
        }
        int currentlevel = CmsResource.getPathLevel(folder);
        if (currentlevel < endlevel) {
            endlevel = currentlevel;
        }
        if (startlevel == endlevel) {
            return getNavigationForFolder(cms, CmsResource.getPathPart(folder, startlevel), startlevel);
        }

        List<CmsJspNavElement> result = new ArrayList<CmsJspNavElement>();
        float parentcount = 0;

        for (int i = startlevel; i <= endlevel; i++) {
            String currentfolder = CmsResource.getPathPart(folder, i);
            List<CmsJspNavElement> entries = getNavigationForFolder(cms, currentfolder);
            // Check for parent folder
            if (parentcount > 0) {
                for (CmsJspNavElement e : entries) {
                    e.setNavPosition(e.getNavPosition() + parentcount);
                }
            }
            // Add new entries to result
            result.addAll(entries);
            Collections.sort(result);
            // Finally spread the values of the navigation items so that there is enough room for further items
            float pos = 0;
            int count = 0;
            String nextfolder = CmsResource.getPathPart(folder, i + 1);
            parentcount = 0;
            for (CmsJspNavElement e : result) {
                pos = 10000 * (++count);
                e.setNavPosition(pos);
                if (e.getResourceName().startsWith(nextfolder)) {
                    parentcount = pos;
                }
            }
            if (parentcount == 0) {
                parentcount = pos;
            }
        }
        return result;
    }

    /**
     * This method builds a complete navigation tree with entries of all branches 
     * from the specified folder.<p>
     * 
     * For an unlimited depth of the navigation (i.e. no <code>endLevel</code>), 
     * set the <code>endLevel</code> to a value &lt; 0.<p>
     * 
     * 
     * @param cms the current CMS context
     * @param folder the root folder of the navigation tree
     * @param endLevel the end level of the navigation
     * 
     * @return list of navigation elements, in depth first order
     */
    public static List<CmsJspNavElement> getSiteNavigation(CmsObject cms, String folder, int endLevel) {

        // check if a specific end level was given, if not, build the complete navigation
        boolean noLimit = false;
        if (endLevel < 0) {
            noLimit = true;
        }
        List<CmsJspNavElement> list = new ArrayList<CmsJspNavElement>();
        // get the navigation for this folder
        List<CmsJspNavElement> curnav = getNavigationForFolder(cms, folder);
        // loop through all navigation entries
        for (CmsJspNavElement ne : curnav) {
            // add the navigation entry to the result list
            list.add(ne);
            // check if navigation entry is a folder and below the max level -> if so, get the navigation from this folder as well
            if (ne.isFolderLink() && (noLimit || (ne.getNavTreeLevel() < endLevel))) {
                List<CmsJspNavElement> subnav = getSiteNavigation(cms, ne.getResourceName(), endLevel);
                // copy the result of the subfolder to the result list
                list.addAll(subnav);
            }
        }
        return list;
    }

    /**
     * Build a "bread crumb" path navigation to the current folder.<p>
     * 
     * @return ArrayList sorted list of navigation elements
     * 
     * @see #getNavigationBreadCrumb(String, int, int, boolean) 
     */
    public List<CmsJspNavElement> getNavigationBreadCrumb() {

        return getNavigationBreadCrumb(m_requestUriFolder, 0, -1, true);
    }

    /**
     * Build a "bread crumb" path navigation to the current folder.<p>
     * 
     * @param startlevel the start level, if negative, go down |n| steps from selected folder
     * @param currentFolder include the selected folder in navigation or not
     * 
     * @return sorted list of navigation elements
     * 
     * @see #getNavigationBreadCrumb(String, int, int, boolean) 
     */
    public List<CmsJspNavElement> getNavigationBreadCrumb(int startlevel, boolean currentFolder) {

        return getNavigationBreadCrumb(m_requestUriFolder, startlevel, -1, currentFolder);
    }

    /**
     * Build a "bread crumb" path navigation to the current folder.<p>
     * 
     * @param startlevel the start level, if negative, go down |n| steps from selected folder
     * @param endlevel the end level, if -1, build navigation to selected folder
     * 
     * @return sorted list of navigation elements
     * 
     * @see #getNavigationBreadCrumb(String, int, int, boolean) 
     */
    public List<CmsJspNavElement> getNavigationBreadCrumb(int startlevel, int endlevel) {

        return getNavigationBreadCrumb(m_requestUriFolder, startlevel, endlevel, true);
    }

    /** 
     * Build a "bread crumb" path navigation to the given folder.<p>
     * 
     * The <code>startlevel</code> marks the point where the navigation starts from, if negative, 
     * the count of steps to go down from the given folder.<p>
     *  
     * The <code>endlevel</code> is the maximum level of the navigation path, set it to -1 to build the
     * complete navigation to the given folder.<p>
     * 
     * You can include the given folder in the navigation by setting <code>currentFolder</code> to 
     * <code>true</code>, otherwise <code>false</code>.<p> 
     * 
     * @param folder the selected folder
     * @param startlevel the start level, if negative, go down |n| steps from selected folder
     * @param endlevel the end level, if -1, build navigation to selected folder
     * @param currentFolder include the selected folder in navigation or not
     * 
     * @return sorted list of navigation elements
     */
    public List<CmsJspNavElement> getNavigationBreadCrumb(
        String folder,
        int startlevel,
        int endlevel,
        boolean currentFolder) {

        List<CmsJspNavElement> result = new ArrayList<CmsJspNavElement>();

        int level = CmsResource.getPathLevel(folder);
        // decrease folder level if current folder is not displayed
        if (!currentFolder) {
            level -= 1;
        }
        // check current level and change endlevel if it is higher or -1
        if ((level < endlevel) || (endlevel == -1)) {
            endlevel = level;
        }

        // if startlevel is negative, display only |startlevel| links
        if (startlevel < 0) {
            startlevel = endlevel + startlevel + 1;
            if (startlevel < 0) {
                startlevel = 0;
            }
        }

        // create the list of navigation elements     
        for (int i = startlevel; i <= endlevel; i++) {
            String navFolder = CmsResource.getPathPart(folder, i);
            CmsJspNavElement e = getNavigationForResource(navFolder);
            // add element to list
            result.add(e);
        }

        return result;
    }

    /**
     * Collect all navigation elements from the files of the folder of the current request URI.<p>
     *
     * @return a sorted (ascending to navigation position) list of navigation elements.
     */
    public List<CmsJspNavElement> getNavigationForFolder() {

        return getNavigationForFolder(m_cms, m_requestUriFolder);
    }

    /** 
     * Build a navigation for the folder that is either minus levels up 
     * from of the folder of the current request URI, or that is plus levels down from the 
     * root folder towards the current request URI.<p> 
     * 
     * If level is set to zero the root folder is used by convention.<p>
     * 
     * @param level if negative, walk this many levels up, if positive, walk this many 
     *                  levels down from root folder 
     * @return a sorted (ascending to navigation position) list of navigation elements
     */
    public List<CmsJspNavElement> getNavigationForFolder(int level) {

        return getNavigationForFolder(m_cms, m_requestUriFolder, level);
    }

    /**
     * Collect all navigation elements from the files in the given folder.<p>
     *
     * @param folder the selected folder
     * 
     * @return A sorted (ascending to navigation position) list of navigation elements
     */
    public List<CmsJspNavElement> getNavigationForFolder(String folder) {

        return getNavigationForFolder(m_cms, folder);
    }

    /** 
     * Build a navigation for the folder that is either minus levels up 
     * from the given folder, or that is plus levels down from the 
     * root folder towards the given folder.<p> 
     * 
     * If level is set to zero the root folder is used by convention.<p>
     * 
     * @param folder the selected folder
     * @param level if negative, walk this many levels up, if positive, walk this many 
     *                  levels down from root folder 
     *                  
     * @return a sorted (ascending to navigation position) list of navigation elements
     */
    public List<CmsJspNavElement> getNavigationForFolder(String folder, int level) {

        return getNavigationForFolder(m_cms, folder, level);
    }

    /**
     * Returns a navigation element for the resource of the current request URI.<p>
     *  
     * @return a navigation element for the resource of the current request URI
     */
    public CmsJspNavElement getNavigationForResource() {

        return getNavigationForResource(m_cms, m_requestUri);
    }

    /**
     * Returns a navigation element for the named resource.<p>
     * 
     * @param resource the resource name to get the navigation information for, 
     *              must be a full path name, e.g. "/docs/index.html"
     *              
     * @return a navigation element for the given resource
     */
    public CmsJspNavElement getNavigationForResource(String resource) {

        return getNavigationForResource(m_cms, resource);
    }

    /**
     * Builds a tree navigation for the folders between the provided start and end level.<p>
     * 
     * @param startlevel the start level
     * @param endlevel the end level
     * 
     * @return a sorted list of navigation elements with the navigation tree level property set
     *  
     * @see #getNavigationTreeForFolder(CmsObject, String, int, int)
     */
    public List<CmsJspNavElement> getNavigationTreeForFolder(int startlevel, int endlevel) {

        return getNavigationTreeForFolder(m_cms, m_requestUriFolder, startlevel, endlevel);
    }

    /**
     * Builds a tree navigation for the folders between the provided start and end level.<p>
     * 
     * @param folder the selected folder
     * @param startlevel the start level
     * @param endlevel the end level
     * 
     * @return a sorted list of navigation elements with the navigation tree level property set
     *  
     * @see #getNavigationTreeForFolder(CmsObject, String, int, int) 
     */
    public List<CmsJspNavElement> getNavigationTreeForFolder(String folder, int startlevel, int endlevel) {

        return getNavigationTreeForFolder(m_cms, folder, startlevel, endlevel);
    }

    /**
     * This method builds a complete site navigation tree with entries of all branches.<p>
     *
     * @see #getSiteNavigation(CmsObject, String, int)
     * 
     * @return list of navigation elements, in depth first order
     */
    public List<CmsJspNavElement> getSiteNavigation() {

        return getSiteNavigation(m_cms, "/", -1);
    }

    /**
     * This method builds a complete navigation tree with entries of all branches 
     * from the specified folder.<p>
     * 
     * @param folder folder the root folder of the navigation tree
     * @param endLevel the end level of the navigation
     * 
     * @return list of navigation elements, in depth first order
     * 
     * @see #getSiteNavigation(CmsObject, String, int)
     */
    public List<CmsJspNavElement> getSiteNavigation(String folder, int endLevel) {

        return getSiteNavigation(m_cms, folder, endLevel);
    }

    /**
     * Initializes this bean.<p>
     * 
     * @param cms context provider for the current request
     */
    public void init(CmsObject cms) {

        m_cms = cms;
        m_requestUri = m_cms.getRequestContext().getUri();
        m_requestUriFolder = CmsResource.getFolderPath(m_requestUri);
    }
}
