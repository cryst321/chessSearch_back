package org.example.chessearch_back.config;

import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.nio.file.Paths;

@Configuration
public class LuceneConfig {

    private static final Logger log = LoggerFactory.getLogger(LuceneConfig.class);

    private static final String LUCENE_INDEX_PATH = "./lucene-index/";

    private Directory directory;
    private IndexWriter indexWriter;
    private SearcherManager searcherManager;

    /**
     * Creates the Lucene Directory bean, representing the index storage location
     * @return directory
     * @throws IOException if there's an error opening the directory
     */
    @Bean(destroyMethod = "close")
    @Scope("singleton")
    public Directory directory() throws IOException {
        if (this.directory == null) {
            log.info("Initializing Lucene Directory at path: {}", LUCENE_INDEX_PATH);
            this.directory = FSDirectory.open(Paths.get(LUCENE_INDEX_PATH));
        }
        return this.directory;
    }

    /**
     * creates the Analyzer bean
     * @return analyzer
     */
    @Bean
    @Scope("singleton")
    public Analyzer analyzer() {
        return new WhitespaceAnalyzer();
    }

    /**
     * Creates the IndexWriterConfig bean (configures how documents are indexed)
     * sets BM25Similarity.
     * @param analyzer Analyzer bean
     * @return IndexWriterConfig instance
     */
    @Bean
    @Scope("singleton")
    public IndexWriterConfig indexWriterConfig(Analyzer analyzer) {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setSimilarity(new BM25Similarity());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        return config;
    }

    /**
     * Creates the IndexWriter bean to add/update documents in the index
     * @param directory Directory bean
     * @param config IndexWriterConfig bean
     * @return IndexWriter instance
     * @throws IOException if there's an error creating the writer
     */
    @Bean
    @Scope("singleton")
    public IndexWriter indexWriter(Directory directory, IndexWriterConfig config) throws IOException {
        if (this.indexWriter == null) {
            log.info("Initializing Lucene IndexWriter...");
            this.indexWriter = new IndexWriter(directory, config);
        }
        return this.indexWriter;
    }

    /**
     * Creates the SearcherManager bean
     * @param writer The IndexWriter bean
     * @return SearcherManager instance
     * @throws IOException if there's an error creating the manager
     */
    @Bean
    @Scope("singleton")
    public SearcherManager searcherManager(IndexWriter writer) throws IOException {
        if (this.searcherManager == null) {
            log.info("Initializing Lucene SearcherManager...");
            boolean applyAllDeletes = true;
            this.searcherManager = new SearcherManager(writer, applyAllDeletes, false, new SearcherFactory());
        }
        return this.searcherManager;
    }


    /**
     * Ensures Lucene resources are closed when the Spring application context shuts down
     */
    @PreDestroy
    public void closeLuceneResources() {
        log.info("Closing Lucene resources...");
        if (this.searcherManager != null) {
            try {
                this.searcherManager.close();
                log.info("SearcherManager closed.");
            } catch (IOException e) {
                log.error("Error closing SearcherManager", e);
            }
        }
        if (this.indexWriter != null) {
            try {
                 if (indexWriter.hasUncommittedChanges()) {
                     log.info("Committing changes before closing IndexWriter...");
                     indexWriter.commit();
                }
                this.indexWriter.close();
                log.info("IndexWriter closed.");
            } catch (IOException e) {
                log.error("Error closing IndexWriter", e);
            }
        }
        if (this.directory != null) {
            try {
                this.directory.close();
                log.info("Directory closed.");
            } catch (IOException e) {
                log.error("Error closing Directory", e);
            }
        }
    }
}
