<%@include file="/libs/foundation/global.jsp"%><%%><%@page session="false" contentType="application/json; charset=utf-8" 
      pageEncoding="UTF-8"
    import="org.apache.sling.api.resource.*,
    java.util.*,
    javax.jcr.*,
    javax.jcr.query.*"%>
<%!
    public static String encode(String s) {
        if (s == null) {
            return "null";
        }
        int length = s.length();
        if (length == 0) {
            return "\"\"";
        }
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c == '\"' || c == '\\' || c < ' ' || (c >= 0xd800 && c <= 0xdbff)) {
                StringBuilder buff = new StringBuilder(length + 2 + length / 8);
                buff.append('\"');
                escape(s, length, buff);
                return buff.append('\"').toString();
            }
        }
        StringBuilder buff = new StringBuilder(length + 2);
        return buff.append('\"').append(s).append('\"').toString();
    }
    public static void escape(String s, StringBuilder buff) {
        escape(s, s.length(), buff);
    }
    private static void escape(String s, int length, StringBuilder buff) {
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            switch (c) {
            case '"':
                // quotation mark
                buff.append("\\\"");
                break;
            case '\\':
                // backslash
                buff.append("\\\\");
                break;
            case '\b':
                // backspace
                buff.append("\\b");
                break;
            case '\f':
                // formfeed
                buff.append("\\f");
                break;
            case '\n':
                // newline
                buff.append("\\n");
                break;
            case '\r':
                // carriage return
                buff.append("\\r");
                break;
            case '\t':
                // horizontal tab
                buff.append("\\t");
                break;
            default:
                if (c < ' ') {
                    buff.append(String.format("\\u%04x", (int) c));
                } else if (c >= 0xd800 && c <= 0xdbff) {
                    // isSurrogate(), only available in Java 7
                    if (i < length - 1 && Character.isSurrogatePair(c, s.charAt(i + 1))) {
                        // ok surrogate
                        buff.append(c);
                        buff.append(s.charAt(i + 1));
                        i += 1;
                    } else {
                        // broken surrogate -> escape
                        buff.append(String.format("\\u%04x", (int) c));
                    }
                } else {
                    buff.append(c);
                }
            }
        }
    }
%>
<%
    Session session = resourceResolver.adaptTo(Session.class);
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Value lastEntry = session.getValueFactory().createValue("");
    int totalCount = 0;
    String qs = "/jcr:root/content/dam//element(*, dam:Asset)[jcr:content/@contentFragment = true] option(index tag fragments)";
    // javax.jcr.query.Query query = queryManager.createQuery(qs, Query.JCR_SQL2);
    javax.jcr.query.Query query = queryManager.createQuery(qs, "xpath");
    QueryResult result = query.execute();
    %>{ 
    "fragments": {
        <%
    NodeIterator it = result.getNodes();
    HashSet<String> multiple = new HashSet<String>();
    HashSet<String> single = new HashSet<String>();
    while (it.hasNext()) {
        Node n = it.nextNode();
        Node data = n.getNode("jcr:content").getNode("data");
        Node master = data.getNode("master");
        PropertyIterator pit = master.getProperties();
        while (pit.hasNext()) {
            Property p = pit.nextProperty();
            if (!p.isMultiple()) {
                single.add(p.getName());
            }
            if (p.isMultiple() && p.getValues().length > 1) {
                multiple.add(p.getName());
            }
        }
    }

    it = result.getNodes();
    int row = 0;
    TreeSet<String> modelSet = new TreeSet<>();
    while (it.hasNext()) {
        // if (row > 10000) break;
        Node n = it.nextNode();
        Node data = n.getNode("jcr:content").getNode("data");
        String cqModel = data.getProperty("cq:model").getString();
        modelSet.add(cqModel);
        // if(true) continue;
        if (row++ > 0) {
            %>,
        <%
        }
        %>"<%= n.getPath() %>": {
        <%
        %>    "_model": "<%= cqModel %>",
        <%
        for (NodeIterator vit = data.getNodes(); vit.hasNext();) {
            Node variation = vit.nextNode();
            String variationName = variation.getName();
            %>    "_variation": "<%= variationName %>"<%
            PropertyIterator pit = variation.getProperties();
            int pid = 0;
            while (pit.hasNext()) {
                Property p = pit.nextProperty();
                if (p.getName().equals("jcr:primaryType")) {
                    continue;
                }
                if (p.getName().equals("jcr:mixinTypes")) {
                    continue;
                }
                if (p.getName().indexOf("@") >= 0) {
                    continue;
                }
                if (row++ > 0) {
                    %>,
            <%
                }
                if (!p.isMultiple()) {
                    %><%= encode(p.getName()) %>: <%= encode(p.getString()) %><%
                } else {
                    if (multiple.contains(p.getName())) {
                        %><%= encode(p.getName() + "S") %>: [<%
                 	    for(int i=0; i<p.getValues().length; i++) {
                            %><%= (i > 0 ? ", " : "") %><%= encode(p.getValues()[i].getString()) %><%
                        }
                        %>]<%
                	} else {
                        %><%= encode(p.getName()) %>: <%= encode(p.getValues()[0].getString()) %><%
              	    }
                }
            }
            %>
        }<%
        }
    }
    %>
    }, 
    "models": {
        <%
    row = 0;
    for (String model: modelSet) {
        if (row++ > 0) {
            %>,
        <%
        }
        %>"<%= model %>": {
            <%
        Node n = session.getNode(model);
        Node items = n.getNode("jcr:content").getNode("model").getNode("cq:dialog").getNode("content").getNode("items");
        int field = 0;
        for (NodeIterator itemIt = items.getNodes(); itemIt.hasNext();) {
            Node item = itemIt.nextNode();
            if (!item.hasProperty("metaType")) {
                continue;
            }
            String metaType = item.getProperty("metaType").getString();
            if (metaType.equals("tab-placeholder")) {
                continue;
            }
            String valueType = item.getProperty("valueType").getString();
            String name = item.getName();
            if (item.hasProperty("name")) {
                name = item.getProperty("name").getString();
            }
            if (metaType.equals("text-single")) {
            } else if (metaType.equals("text-multi")) {
            } else if (metaType.equals("boolean")) {
            } else if (metaType.equals("tags")) {
            } else if (metaType.equals("fragment-reference")) {
            }
            if (field++ > 0) {
                %>,
            <%
            }
            %>"<%= name %>": {<%
            %>"metaType": "<%= metaType %>", "valueType": "<%= valueType %>" }<%
        }
        %>}<%
    }
%>    
    }
}<%
    out.flush();


/*
Nodes:
/libs/cq/core/components/test (sling:Folder)
  sling:resourceType = /libs/cq/core/components/test

/libs/cq/core/components/test/test.jsp (nt:file)

Execute:
http://localhost:4502/libs/cq/core/components/test.html
*/ 
%>
