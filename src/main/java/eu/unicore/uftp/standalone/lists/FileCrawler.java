package eu.unicore.uftp.standalone.lists;

/**
 *
 * @author jj
 */
public abstract class FileCrawler {

    public enum RecursivePolicy {

        RECURSIVE,
        NONRECURSIVE
    }

    public abstract void crawl(Operation cmd) throws Exception;

    public abstract boolean isSingleFile(String path);

}
