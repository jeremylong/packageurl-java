/*
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.packageurl;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * purl stands for package URL.
 *
 * A purl is a URL composed of seven components:
 *
 * scheme:type/namespace/name@version?qualifiers#subpath
 *
 * Components are separated by a specific character for unambiguous parsing.
 * A purl must NOT contain a URL Authority i.e. there is no support for username,
 * password, host and port components. A namespace segment may sometimes look
 * like a host but its interpretation is specific to a type.
 *
 * SPEC: https://github.com/package-url/purl-spec
 *
 * @author Steve Springett
 * @since 1.0.0
 */
public final class PackageURL implements Serializable {

    private static final long serialVersionUID = 3243226021636427586L;
    private static final Pattern TYPE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9.+-]+$");
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9.-_]+$");

    /**
     * Constructs a new PackageURL object by parsing the specified string.
     * @param purl a valid package URL string to parse
     * @throws MalformedPackageURLException if parsing fails
     * @since 1.0.0
     */
    public PackageURL(String purl) throws MalformedPackageURLException {
        parse(purl);
    }

    /**
     * Constructs a new PackageURL object by specifying only the required
     * parameters necessary to create a valid PackageURL.
     * @param type the type of package (i.e. maven, npm, gem, etc)
     * @param name the name of the package
     * @throws MalformedPackageURLException if parsing fails
     * @since 1.0.0
     */
    public PackageURL(String type, String name) throws MalformedPackageURLException {
        this(type, null, name, null, null, null);
    }

    /**
     * Constructs a new PackageURL object.
     * @param type the type of package (i.e. maven, npm, gem, etc)
     * @param namespace the name prefix (i.e. group, owner, organization)
     * @param name the name of the package
     * @param version the version of the package
     * @param qualifiers an array of key/value pair qualifiers
     * @param subpath the subpath string
     * @throws MalformedPackageURLException if parsing fails
     * @since 1.0.0
     */
    public PackageURL(String type, String namespace, String name, String version, TreeMap<String, String> qualifiers, String subpath)
            throws MalformedPackageURLException {

        this.scheme = validateScheme("pkg");
        this.type = validateType(type);
        this.namespace = validateNamespace(namespace);
        this.name = validateName(name);
        this.version = validateVersion(version);
        this.qualifiers = validateQualifiers(qualifiers);
        this.subpath = validateSubpath(subpath);
    }

    /**
     * The PackageURL scheme constant
     */
    private String scheme;

    /**
     * The package "type" or package "protocol" such as maven, npm, nuget, gem, pypi, etc.
     * Required.
     */
    private String type;

    /**
     * The name prefix such as a Maven groupid, a Docker image owner, a GitHub user or organization.
     * Optional and type-specific.
     */
    private String namespace;

    /**
     * The name of the package.
     * Required.
     */
    private String name;

    /**
     * The version of the package.
     * Optional.
     */
    private String version;

    /**
     * Extra qualifying data for a package such as an OS, architecture, a distro, etc.
     * Optional and type-specific.
     */
    private Map<String, String> qualifiers;

    /**
     * Extra subpath within a package, relative to the package root.
     * Optional.
     */
    private String subpath;

    /**
     * Returns the package url scheme.
     * @return the scheme
     * @since 1.0.0
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Returns the package "type" or package "protocol" such as maven, npm, nuget, gem, pypi, etc.
     * @return the type
     * @since 1.0.0
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the name prefix such as a Maven groupid, a Docker image owner, a GitHub user or organization.
     * @return the namespace
     * @since 1.0.0
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the name of the package.
     * @return the name of the package
     * @since 1.0.0
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the version of the package.
     * @return the version of the package
     * @since 1.0.0
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns extra qualifying data for a package such as an OS, architecture, a distro, etc.
     * @return qualifiers
     * @since 1.0.0
     */
    public Map getQualifiers() {
        return qualifiers;
    }

    /**
     * Returns extra subpath within a package, relative to the package root.
     * @return the subpath
     * @since 1.0.0
     */
    public String getSubpath() {
        return subpath;
    }

    /**
     * Given a specified PackageURL, this method will parse the purl and populate this classes
     * instance fields so that the corresponding getters may be called to retrieve the individual
     * pieces of the purl.
     * @param purl the purl string to parse
     * @throws MalformedPackageURLException if an exception occurs when parsing
     */
    private void parse(String purl) throws MalformedPackageURLException {
        if (purl == null || "".equals(purl.trim())) {
            throw new MalformedPackageURLException("Invalid purl: Contains an empty or null value");
        }

        try {
            URI uri = new URI(purl);
            // Check to ensure that none of these parts are parsed. If so, it's an invalid purl.
            if (uri.getUserInfo() != null || uri.getPort() != -1)  {
                throw new MalformedPackageURLException("Invalid purl: Contains parts not supported by the purl spec");
            }

            this.scheme = validateScheme(uri.getScheme());

            // This is the purl (minus the scheme) that needs parsed.
            String remainder = purl.substring(4);

            if (remainder.contains("#")) { // subpath is optional - check for existence
                final int index = remainder.lastIndexOf("#");
                this.subpath = validateSubpath(remainder.substring(index + 1));
                remainder = remainder.substring(0, index);
            }

            if (remainder.contains("?")) { // qualifiers are optional - check for existence
                final int index = remainder.lastIndexOf("?");
                this.qualifiers = validateQualifiers(remainder.substring(index + 1));
                remainder = remainder.substring(0, index);
            }

            if (remainder.contains("@")) { // version is optional - check for existence
                final int index = remainder.lastIndexOf("@");
                this.version = validateVersion(remainder.substring(index + 1));
                remainder = remainder.substring(0, index);
            }

            // The 'remainder' should now consist of the type, an optional namespace, and the name

            // Strip zero or more leading '/' from the beginning ('type')
            remainder = remainder.replaceAll("^[/]*", "");

            String[] firstPartArray = remainder.split("/");
            if (firstPartArray.length < 2) { // The array must contain a 'type' and a 'name' at minimum
                throw new MalformedPackageURLException("Invalid purl: Does not contain a minimum of a 'type' and a 'name'");
            }

            this.type = validateType(firstPartArray[0]);
            this.name = validateName(firstPartArray[firstPartArray.length - 1]);

            // Test for namespaces
            if (firstPartArray.length > 2) {
                String[] namespaces = Arrays.copyOfRange(firstPartArray, 1, firstPartArray.length - 1);
                String namespace = String.join(",", namespaces);
                this.namespace = validateNamespace(namespace);
            }

        } catch (URISyntaxException e) {
            throw new MalformedPackageURLException("Invalid purl: " + e.getMessage());
        }
    }

    private String validateScheme(String scheme) throws MalformedPackageURLException {
        if (scheme == null || !scheme.equals("pkg")) {
            throw new MalformedPackageURLException("The PackageURL scheme is invalid");
        }
        return scheme;
    }

    private String validateType(String type) throws MalformedPackageURLException {
        if (type == null || !TYPE_PATTERN.matcher(type).matches()) {
            throw new MalformedPackageURLException("The PackageURL type specified is invalid");
        }
        return type.toLowerCase();
    }

    private String validateNamespace(String value) {
        if (value == null) {
            return null;
        }
        String temp;
        switch (type) {
            case StandardTypes.BITBUCKET:
            case StandardTypes.DEBIAN:
            case StandardTypes.GITHUB:
            case StandardTypes.GOLANG:
            case StandardTypes.NPM:
            case StandardTypes.RPM:
                temp = value.toLowerCase();
                break;
            default:
                temp = value;
                break;
        }
        return urldecode(temp);
    }

    private String validateName(String value) throws MalformedPackageURLException {
        if (value == null) {
            throw new MalformedPackageURLException("The PackageURL name specified is invalid");
        }
        String temp;
        switch (type) {
            case StandardTypes.BITBUCKET:
            case StandardTypes.DEBIAN:
            case StandardTypes.GITHUB:
            case StandardTypes.GOLANG:
            case StandardTypes.NPM:
                temp = value.toLowerCase();
                break;
            case StandardTypes.PYPI:
                temp = value.replaceAll("_", "-").toLowerCase();
                break;
            default:
                temp = value;
                break;
        }
        return urldecode(temp);
    }

    private String validateVersion(String version) {
        if (version == null) {
            return null;
        }
        return urldecode(version);
    }

    private Map<String, String> validateQualifiers(String encodedString) throws MalformedPackageURLException {
        final Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final String[] pairs =  encodedString.split("&");
        for (String pair : pairs) {
            if (pair.contains("=")) {
                final String[] kvpair = pair.split("=");
                if (kvpair.length == 2) {
                    map.put(validateQualifierKey(kvpair[0]), urldecode(kvpair[1]));
                }
            }
        }
        return map;
    }

    private Map<String, String> validateQualifiers(Map<String, String> qualifiers) throws MalformedPackageURLException {
        if (qualifiers == null) {
            return null;
        }
        for (String key: qualifiers.keySet()) {
            validateQualifierKey(key);
            if (qualifiers.get(key) == null) {
                throw new MalformedPackageURLException("The PackageURL specified contains a qualifier key with a null value");
            }
        }
        return qualifiers;
    }

    private String validateQualifierKey(String key) throws MalformedPackageURLException {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new MalformedPackageURLException("The PackageURL specified contains a qualifier key name which is invalid");
        }
        return key;
    }

    private String validateSubpath(String subpath) {
        if (subpath == null) {
            return null;
        }
        return urldecode(stripLeadingAndTrailingSlash(subpath)); // leading and trailing slashes always need to be removed
    }

    /**
     * Returns a canonicalized representation of the purl.
     * @return a canonicalized representation of the purl
     * @since 1.0.0
     */
    public String canonicalize() {
        final StringBuilder purl = new StringBuilder();
        purl.append(scheme).append(":");
        if (type != null) {
            purl.append(type);
        }
        purl.append("/");
        if (namespace != null) {
            purl.append(urlencode(namespace));
            purl.append("/");
        }
        if (name != null) {
            purl.append(urlencode(name));
        }
        if (version != null) {
            purl.append("@").append(urlencode(version));
        }
        if (qualifiers != null && qualifiers.size() > 0) {
            purl.append("?");
            for (Map.Entry<String, String> entry : qualifiers.entrySet()) {
                purl.append(entry.getKey().toLowerCase());
                purl.append("=");
                purl.append(urlencode(entry.getValue()));
                purl.append("&");
            }
            purl.setLength(purl.length() - 1);
        }
        if (subpath != null) {
            purl.append("#").append(subpath);
        }
        return purl.toString();
    }

    /**
     * Removes leading and trailing '/' characters from the specified input.
     * @param input the String to remove leading and trailing '/' from
     * @return the processed String
     */
    private String stripLeadingAndTrailingSlash(String input) {
        if (input == null) {
            return null;
        }
        if (input.startsWith("/")) {
            input = input.substring(1);
        }
        if (input.endsWith("/")) {
            input = input.substring(0, input.length() -1);
        }
        return input;
    }

    /**
     * Encodes the input in conformance with RFC-3986.
     * @param input the String to encode
     * @return an encoded String
     */
    private String urlencode(String input) {
        try {
            // This SHOULD encoded according to RFC-3986 because URLEncoder alone does not.
            return URLEncoder.encode(input, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            return input; // this should never occur
        }
    }

    /**
     * Optionally decodes a String, if it's encoded. If String is not encoded,
     * method will return the original input value.
     * @param input the value String to decode
     * @return a decoded String
     */
    private String urldecode(String input) {
        if (input == null) {
            return null;
        }
        try {
            final String decoded = URLDecoder.decode(input, StandardCharsets.UTF_8.name());
            if (!decoded.equals(input)) {
                return decoded;
            }
        } catch (UnsupportedEncodingException e) {
            return input; // this should never occur
        }
        return input;
    }

    /**
     * Convenience constants that defines common PackageURL 'type's.
     * @since 1.0.0
     */
    public static class StandardTypes {
        public static final String BITBUCKET = "bitbucket";
        public static final String COMPOSER = "composer";
        public static final String DEBIAN = "deb";
        public static final String DOCKER = "docker";
        public static final String GEM = "gem";
        public static final String GENERIC = "generic";
        public static final String GITHUB = "github";
        public static final String GOLANG = "golang";
        public static final String MAVEN = "maven";
        public static final String NPM = "npm";
        public static final String NUGET = "nuget";
        public static final String PYPI = "pypi";
        public static final String RPM = "rpm";
    }

}
