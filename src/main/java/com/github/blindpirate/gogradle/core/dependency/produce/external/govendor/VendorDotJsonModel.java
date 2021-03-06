package com.github.blindpirate.gogradle.core.dependency.produce.external.govendor;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.blindpirate.gogradle.util.Assert;
import com.github.blindpirate.gogradle.util.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.blindpirate.gogradle.util.StringUtils.isBlank;

/**
 * Model of vendor/vendor.json in repos managed by govendor.
 *
 * @see <a href="https://github.com/kardianos/govendor/blob/master/vendor/vendor.json">vendor/vendor.json</a>
 */
@SuppressWarnings({"checkstyle:membername", "checkstyle:parametername"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class VendorDotJsonModel {
    @JsonProperty("comment")
    private String comment;
    @JsonProperty("ignore")
    private String ignore;
    @JsonProperty("rootPath")
    private String rootPath;

    @JsonProperty("package")
    private List<PackageBean> packageX;

    public List<Map<String, Object>> toNotations() {
        return packageX.stream().map(PackageBean::toNotation).collect(Collectors.toList());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PackageBean {
        @JsonProperty("checksumSHA1")
        private String checksumSHA1;
        @JsonProperty("path")
        private String path;
        @JsonProperty("revision")
        private String revision;
        @JsonProperty("revisionTime")
        private String revisionTime;

        Map<String, Object> toNotation() {
            Assert.isNotBlank(path);
            /*
             {
                 "path": "appengine",
                 "revision": ""
             },
             {
                 "path": "appengine_internal",
                 "revision": ""
             }
             */
            return MapUtils.asMapWithoutNull("name", path,
                    "version", isBlank(revision) ? null : revision);
        }
    }
}
