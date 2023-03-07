package matypist.openstud.driver.core.internals;

import matypist.openstud.driver.core.models.Event;
import matypist.openstud.driver.core.models.News;
import matypist.openstud.driver.exceptions.OpenstudConnectionException;
import matypist.openstud.driver.exceptions.OpenstudInvalidResponseException;

import java.util.List;

public interface NewsHandler {
    List<News> getNews(String locale, boolean withDescription, Integer limit, Integer page, Integer maxPage,
                       String query) throws OpenstudInvalidResponseException, OpenstudConnectionException;

    List<Event> getNewsletterEvents() throws OpenstudInvalidResponseException, OpenstudConnectionException;
}

