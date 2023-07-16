package com.alflabs.rig4.exp;

import com.alflabs.utils.RPair;
import org.jsoup.nodes.Element;

import java.util.Map;
import java.util.TreeMap;

class CssStyles {
    private final TreeMap<String, String> mMap = new TreeMap<>();

    public CssStyles() {
    }

    public CssStyles(String attrStyle) {
        parseStyle(attrStyle);
    }

    public CssStyles(CssStyles styles) {
        addAll(styles);
    }

    private void addAll(CssStyles styles) {
        mMap.putAll(styles.mMap);
    }

    public void add(String kvStyle) {
        String[] kv = kvStyle.trim().split(":");
        String key = kv[0].trim();
        String value = kv.length < 2 ? "" : kv[1].trim();
        mMap.put(key, value);
    }

    public void parseStyle(String attrStyle) {
        if (attrStyle != null && !attrStyle.isEmpty()) {
            for (String s : attrStyle.split(";")) {
                add(s.trim());
            }
        }
    }

    public RPair<CssStyles, String> deltaChildStyle(String attrStyle) {
        CssStyles deltaChildStyles = null;

        if (attrStyle != null && !attrStyle.isEmpty()) {
            for (String s : attrStyle.split(";")) {
                String[] kv = s.split(":");
                String key = kv[0].trim();
                String value = kv.length < 2 ? "" : kv[1].trim();
                boolean same = mMap.containsKey(key) && mMap.get(key).equals(value);
                if (!same) {
                    if (deltaChildStyles == null) {
                        deltaChildStyles = new CssStyles();
                    }
                    deltaChildStyles.add(s);
                }
            }
        }

        if (deltaChildStyles == null) {
            return null;
        } else {
            CssStyles newParentStyles = new CssStyles(this);
            newParentStyles.addAll(deltaChildStyles);

            return RPair.create(newParentStyles, deltaChildStyles.generateStyle());
        }
    }

    public String generateStyle() {
        StringBuilder sb = new StringBuilder();
        boolean semi = false;
        for (Map.Entry<String, String> entry : mMap.entrySet()) {
            if (semi) {
                sb.append(';');
            }
            sb.append(entry.getKey()).append(':').append(entry.getValue());
            semi = true;
        }
        return sb.toString();
    }

    public void remove(String name) {
        mMap.remove(name);
    }

    public boolean has(String name) {
        return mMap.containsKey(name);
    }

    public String get(String name) {
        return mMap.get(name);
    }

    public int getIntValue(String name, int missingValue) {
        return HtmlTransformer.getIntValue(mMap.get(name), missingValue);
    }

    public void applyTo(Element element) {
        String style = generateStyle();
        if (style.isEmpty()) {
            element.removeAttr(HtmlTransformer.ATTR_STYLE);
        } else {
            element.attr(HtmlTransformer.ATTR_STYLE, style);
        }
    }
}
