package com.adobe.franklin.fragments.extractor;


import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.index.lucene.IndexTracker;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.LuceneIndexProvider;
import org.apache.jackrabbit.oak.plugins.index.lucene.hybrid.DocumentQueue;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.query.QueryIndexProvider;
import org.apache.jackrabbit.oak.spi.state.NodeStore;
import org.apache.jackrabbit.oak.stats.StatisticsProvider;
import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.adobe.franklin.fragments.converter.Json;
import com.adobe.franklin.fragments.extractor.oak.NodeStoreFixture;
import com.adobe.franklin.fragments.extractor.oak.NodeStoreFixtureProvider;
import com.adobe.franklin.fragments.extractor.oak.Options;
import com.adobe.franklin.fragments.utils.ProgressLogger;
import com.google.common.util.concurrent.MoreExecutors;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class FragmentExtractor {
    
    public Json extract(String segmentStore, String blobStore, String user, String password) {

        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.OFF);
        
        try {
            String[] args = new String[] { "--user", user, "--password", password, 
                    "--fds-path", blobStore,
                    segmentStore, 
                    "--read-write" };
            return extract(args);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    private Json extract(String... args) throws Exception {
        OptionParser parser = new OptionParser();
        OptionSpec<String> userOption = parser
                .accepts("user", "User name").withOptionalArg()
                .defaultsTo("admin");
        OptionSpec<String> passwordOption = parser
                .accepts("password", "Password").withOptionalArg()
                .defaultsTo("admin");

        Options oakOptions = new Options();
        OptionSet options = oakOptions.parseAndConfigure(parser, args);

        ProgressLogger.logMessage("Opening nodestore");
        NodeStoreFixture nodeStoreFixture = NodeStoreFixtureProvider.create(oakOptions);

        NodeStore nodeStore = nodeStoreFixture.getStore();
        String user = userOption.value(options);
        String password = passwordOption.value(options);
        Session session = openSession(nodeStore, user, password);
        ProgressLogger.logDone();
        
        try {
            return process(session);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }
    
    public Json process(Session session) throws Exception {
        Json json = new Json(new JSONObject());
        
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        String qs = "/jcr:root/content/dam//element(*, dam:Asset)[jcr:content/@contentFragment = true] option(index tag fragments)";
        ProgressLogger.logMessage("Loading indexes");
        Query query = queryManager.createQuery(qs, "xpath");
        QueryResult result = query.execute();
        Json fragments = json.addChild("fragments");
        NodeIterator it = result.getNodes();
        ProgressLogger.logDone();
        ProgressLogger.logMessage("Reading fragments");
        it = result.getNodes();
        TreeSet<String> modelSet = new TreeSet<>();
        while (it.hasNext()) {
            Node n = it.nextNode();
            Node data = n.getNode("jcr:content").getNode("data");
            String cqModel = data.getProperty("cq:model").getString();
            modelSet.add(cqModel);
            Json fragment = fragments.addChild(n.getPath());
            fragment.setStringProperty("_model", cqModel);
            for (NodeIterator vit = data.getNodes(); vit.hasNext();) {
                Node variation = vit.nextNode();
                String variationName = variation.getName();
                fragment.setStringProperty("_variation", variationName);
                PropertyIterator pit = variation.getProperties();
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
                    if (!p.isMultiple()) {
                        fragment.setStringProperty(p.getName(), p.getString());
                    } else {
                        List<String> list = new ArrayList<>();
                        for(int i=0; i<p.getValues().length; i++) {
                            list.add(p.getValues()[i].getString());
                        }
                        fragment.setStringArray(p.getName(), list);
                    }
                }
            }
        }
        Json models = json.addChild("models");
        for (String model: modelSet) {
            Json modelJson = models.addChild(model);
            Node n = session.getNode(model);
            Node items = n.getNode("jcr:content").getNode("model").getNode("cq:dialog").getNode("content").getNode("items");
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
                Json fieldJson = modelJson.addChild(name);
                fieldJson.setStringProperty("metaType", metaType);
                fieldJson.setStringProperty("valueType", valueType);
            }
        }
        ProgressLogger.logDone();
        return json;
    }

    public static Session openSession(NodeStore nodeStore, String user, String password) throws RepositoryException {
        if (nodeStore == null) {
            return null;
        }
        StatisticsProvider statisticsProvider = StatisticsProvider.NOOP;
        Oak oak = new Oak(nodeStore).with(ManagementFactory.getPlatformMBeanServer());
        oak.getWhiteboard().register(StatisticsProvider.class, statisticsProvider, Collections.emptyMap());
        LuceneIndexProvider provider = createLuceneIndexProvider();
        oak.with((QueryIndexProvider) provider)
                .with((Observer) provider)
                .with(createLuceneIndexEditorProvider());
        Jcr jcr = new Jcr(oak);
        Repository repository = jcr.createRepository();
        return repository.login(new SimpleCredentials(user, password.toCharArray()));
    }
    
    private static LuceneIndexProvider createLuceneIndexProvider() {
        return new LuceneIndexProvider();
    }    
    
    private static LuceneIndexEditorProvider createLuceneIndexEditorProvider() {
        LuceneIndexEditorProvider ep = new LuceneIndexEditorProvider();
        ScheduledExecutorService executorService = MoreExecutors.getExitingScheduledExecutorService(
                (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(5));
        StatisticsProvider statsProvider = StatisticsProvider.NOOP;
        int queueSize = Integer.getInteger("queueSize", 1000);
        long queueTimeout = Long.getLong("queueTimeoutMillis", 100);
        IndexTracker tracker = new IndexTracker();
        DocumentQueue queue = new DocumentQueue(queueSize, queueTimeout, tracker, executorService, statsProvider);
        ep.setIndexingQueue(queue);
        return ep;
    }
    
}
