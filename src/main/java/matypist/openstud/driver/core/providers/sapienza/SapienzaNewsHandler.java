package matypist.openstud.driver.core.providers.sapienza;

import matypist.openstud.driver.core.Openstud;
import matypist.openstud.driver.core.OpenstudHelper;
import matypist.openstud.driver.core.internals.NewsHandler;
import matypist.openstud.driver.core.models.Event;
import matypist.openstud.driver.core.models.EventType;
import matypist.openstud.driver.core.models.News;
import matypist.openstud.driver.exceptions.OpenstudConnectionException;
import matypist.openstud.driver.exceptions.OpenstudInvalidResponseException;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;
import org.threeten.bp.format.DateTimeParseException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public class SapienzaNewsHandler implements NewsHandler {
    private Openstud os;

    public SapienzaNewsHandler(Openstud os) {
        this.os = os;
    }

    @Override
    public List<News> getNews(String locale, boolean withDescription, Integer limit, Integer page, Integer maxPage, String query) throws OpenstudInvalidResponseException, OpenstudConnectionException {
        if (limit == null && page == null && maxPage == null)
            throw new IllegalStateException("limit, page and maxpage can't be all null");
        return _getNews(locale, withDescription, limit, page, maxPage, query);
    }

    private List<News> _getNews(String locale, boolean withDescription, Integer limit, Integer page, Integer maxPage, String query) throws OpenstudConnectionException, OpenstudInvalidResponseException {
        if (locale == null)
            locale = "en";
        Locale localeFormatter;
        if (locale.toLowerCase().equals("it")) localeFormatter = Locale.ITALIAN;
        else localeFormatter = Locale.ENGLISH;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy").withLocale(localeFormatter);
        try {
            List<News> ret = new LinkedList<>();
            int startPage = 0;
            int endPage = maxPage == null ? 1 : maxPage;
            if (page != null) {
                startPage = page;
                endPage = startPage + 1;
            }
            String website_url = "https://www.uniroma1.it";
            String page_key = "page";
            String query_key = "search_api_views_fulltext";
            boolean shouldStop = false;
            int iterations = 0;
            int miss = 0;
            for (int i = startPage; i < endPage && !shouldStop; i++) {
                HttpUrl.Builder urlBuilder = HttpUrl.parse(String.format("%s/%s/tutte-le-notizie", website_url, locale)).newBuilder();
                urlBuilder.addQueryParameter(page_key, i + "");
                if (query != null) {
                    urlBuilder.addQueryParameter(query_key, query);
                }

                Request request = new Request.Builder().url(urlBuilder.build()).get().build();
                Document doc;
                try (Response response = os.getClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                    doc = Jsoup.parse(response.body().string(), request.url().toString());
                }

                Elements boxes = doc.getElementsByClass("box-news");
                for (Element box : boxes) {
                    News news = new News();
                    news.setTitle(box.getElementsByTag("img").attr("title"));
                    // handle empty news
                    if (news.getTitle().isEmpty())
                        continue;
                    news.setLocale(locale);
                    news.setUrl(website_url + box.getElementsByTag("a").attr("href").trim());
                    news.setSmallImageUrl(box.getElementsByTag("img").attr("src"));
                    ret.add(news);
                    if (limit != null && ret.size() >= limit) {
                        shouldStop = true;
                        break;
                    }
                }
                if (boxes.isEmpty()) miss++;
                iterations++;
            }
            if (iterations == miss) {
                OpenstudInvalidResponseException invalidResponse = new OpenstudInvalidResponseException("invalid HTML").setHTMLType();
                os.log(Level.SEVERE, invalidResponse);
                throw invalidResponse;
            }
            LinkedList<News> ignored = new LinkedList<>();
            for (News news : ret) {
                if (!OpenstudHelper.isValidUrl(news.getUrl())) {
                    ignored.add(news);
                    continue;
                }

                Request detailsRequest = new Request.Builder().url(news.getUrl()).get().build();
                Document doc;
                try (Response detailsResponse = os.getClient().newCall(detailsRequest).execute()) {
                    if (!detailsResponse.isSuccessful()) {
                        ignored.add(news); // If fetching details fails, ignore this news item
                        continue;
                    }
                    doc = Jsoup.parse(detailsResponse.body().string(), news.getUrl());
                } catch (IOException e) {
                    ignored.add(news); // Also ignore on connection failure
                    continue;
                }

                if (withDescription) {
                    Element start = doc.getElementsByAttributeValueEnding("class", "testosommario").first();
                    if (start != null)
                        news.setDescription(start.getElementsByClass("field-item even").first().text());
                }
                Element date = doc.getElementsByClass("date-display-single").first();
                if (date != null) {
                    try {
                        news.setDate(LocalDate.parse(date.text().substring(date.text().indexOf(",") + 1).trim(), formatter));
                    } catch (DateTimeParseException e) {
                        e.printStackTrace();
                    }
                }
                news.setImageUrl(doc.getElementsByClass("img-responsive").attr("src"));
            }
            ret.removeAll(ignored);
            return ret;

        } catch (IOException e) {
            OpenstudConnectionException connectionException = new OpenstudConnectionException(e);
            os.log(Level.SEVERE, connectionException);
            throw connectionException;
        }
    }

    @Override
    public List<Event> getNewsletterEvents() throws OpenstudInvalidResponseException, OpenstudConnectionException {
        return _getNewsletterEvents();
    }

    private List<Event> _getNewsletterEvents() throws OpenstudInvalidResponseException, OpenstudConnectionException {
        try {
            List<Event> ret = new LinkedList<>();

            String website_url = "https://www.uniroma1.it/it/newsletter";

            Request request = new Request.Builder().url(website_url).get().build();
            Document doc;
            try (Response response = os.getClient().newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                doc = Jsoup.parse(response.body().string(), website_url);
            }

            Elements events = doc.getElementsByClass("event");
            os.log(Level.FINE, "SapienzaNewsHandler: Found " + events.size() + " event elements.");

            String failureReason = null;
            boolean allFailedForSameReason = true;
            Throwable firstEncounteredException = null;

            DateTimeFormatter detailFormatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("eeee d MMMM yyyy 'alle' HH:mm")
                    .toFormatter(Locale.ITALIAN);

            int failed = 0;
            for (int i = 0; i < events.size(); i++) {
                Element event = events.get(i);
                Element titleEl = event.getElementsByClass("views-field-solr-document").first();

                // Only extract title and URL from the list page
                if (titleEl == null) {
                    os.log(Level.WARNING, "SapienzaNewsHandler: Skipping event " + i + ": missing required title element.");
                    String currentReason = "missing required elements";
                    if (failureReason == null) {
                        failureReason = currentReason;
                        firstEncounteredException = new OpenstudInvalidResponseException("Missing required elements on event " + i);
                    }
                    else if (allFailedForSameReason && !failureReason.equals(currentReason)) allFailedForSameReason = false;
                    failed++;
                    continue;
                }

                Element titleA = titleEl.getElementsByTag("a").first();

                if (titleA == null) {
                    os.log(Level.WARNING, "SapienzaNewsHandler: Skipping event " + i + ": missing required <a> tag within title.");
                    String currentReason = "missing required a tags";
                    if (failureReason == null) {
                        failureReason = currentReason;
                        firstEncounteredException = new OpenstudInvalidResponseException("Missing required <a> tags on event " + i);
                    }
                    else if (allFailedForSameReason && !failureReason.equals(currentReason)) allFailedForSameReason = false;
                    failed++;
                    continue;
                }

                String eventTitle = titleA.text();
                String eventUrl = titleA.attr("href");

                if (!OpenstudHelper.isValidUrl(eventUrl)) {
                    os.log(Level.WARNING, "SapienzaNewsHandler: Skipping event " + i + ": Invalid URL '" + eventUrl + "'");
                    String currentReason = "invalid URL";
                    if (failureReason == null) {
                        failureReason = currentReason;
                        firstEncounteredException = new OpenstudInvalidResponseException("Invalid URL on event " + i + ": " + eventUrl);
                    }
                    else if (allFailedForSameReason && !failureReason.equals(currentReason)) allFailedForSameReason = false;
                    failed++;
                    continue;
                }

                // Fetch detail page for every event
                Request eventRequest = new Request.Builder().url(eventUrl).get().build();
                Document eventDoc;
                try (Response eventResponse = os.getClient().newCall(eventRequest).execute()) {
                    if (!eventResponse.isSuccessful()) {
                        os.log(Level.WARNING, "SapienzaNewsHandler: Skipping event " + i + ". Failed to fetch details page: " + eventResponse.code() + " for URL: " + eventUrl);
                        String currentReason = "failed to fetch details page";
                        if (failureReason == null) {
                            failureReason = currentReason;
                            firstEncounteredException = new OpenstudConnectionException("Failed to fetch details page: " + eventResponse.code() + " for URL: " + eventUrl);
                        }
                        else if (allFailedForSameReason && !failureReason.equals(currentReason)) allFailedForSameReason = false;
                        failed++;
                        continue;
                    }
                    eventDoc = Jsoup.parse(eventResponse.body().string(), eventUrl);
                } catch (IOException e) {
                    os.log(Level.WARNING, "SapienzaNewsHandler: Skipping event " + i + ". IOException fetching details page for URL: " + eventUrl + " " + e.getMessage());
                    String currentReason = "IOException fetching details page";
                    if (failureReason == null) {
                        failureReason = currentReason;
                        firstEncounteredException = e;
                    }
                    else if (allFailedForSameReason && !failureReason.equals(currentReason)) allFailedForSameReason = false;
                    failed++;
                    continue;
                }

                // Get common info from detail page
                String imageUrl = null;
                Element image = eventDoc.getElementsByClass("field-type-image").first();
                if (image != null) {
                    Element imgTag = image.getElementsByTag("img").first();
                    if (imgTag != null)
                        imageUrl = imgTag.attr("src");
                }
                String descriptionText = null;
                Element description = eventDoc.getElementsByClass("article-body").first();
                if (description != null) descriptionText = description.text();

                // Find all individual date/time/place blocks
                // This selector targets the rows inside .view-date-and-place
                Elements dateBlocks = eventDoc.select(".view-date-and-place .view-content > .views-row");

                if (dateBlocks.isEmpty()) {
                    os.log(Level.WARNING, "SapienzaNewsHandler: No date/time blocks found for event " + eventUrl + ". Skipping.");
                    continue; // Skip to the next event from the list
                }

                boolean addedAtLeastOne = false;
                for (Element block : dateBlocks) {
                    // Create a new Event object for each block
                    Event multiEvent = new Event(EventType.THEATRE);
                    multiEvent.setTitle(eventTitle);
                    multiEvent.setUrl(eventUrl);
                    multiEvent.setImageUrl(imageUrl);
                    multiEvent.setDescription(descriptionText);

                    Element multiDateEl = block.select(".views-field-php.field-apm-date .field-content").first();
                    Element multiRoomEl = block.select(".views-field-field-apm-aula .field-content").first();
                    Element multiWhereEl = block.select(".views-field-field-apm-edificio .field-content").first();

                    if (multiDateEl == null) {
                        os.log(Level.WARNING, "SapienzaNewsHandler: Skipping date entry for " + eventUrl + ": missing date element in block.");
                        continue;
                    }

                    String dateString = multiDateEl.text().trim();

                    try {
                        multiEvent.setStart(LocalDateTime.parse(dateString, detailFormatter));
                    } catch (DateTimeParseException e) {
                        os.log(Level.WARNING, "SapienzaNewsHandler: Skipping date entry for " + eventUrl + ". DateTimeParseException for string: '" + dateString + "' " + e.getMessage());
                        continue;
                    }

                    if (multiRoomEl != null) {
                        multiEvent.setRoom(multiRoomEl.text().trim().replaceAll(" ?- ?", ", "));
                    }
                    if (multiWhereEl != null) {
                        multiEvent.setWhere(multiWhereEl.text().trim());
                    }

                    ret.add(multiEvent);
                    addedAtLeastOne = true;
                }

                if (!addedAtLeastOne) {
                    os.log(Level.WARNING, "SapienzaNewsHandler: Found " + dateBlocks.size() + " date blocks for " + eventUrl + " but failed to parse any of them.");
                }
            }

            if (failed == events.size() && !events.isEmpty()) {
                os.log(Level.SEVERE, "SapienzaNewsHandler: All " + failed + " event parsing attempts failed. Throwing InvalidResponseException.");
                String exceptionReason = failureReason;
                if (!allFailedForSameReason) {
                    exceptionReason = "multiple reasons";
                } else if (exceptionReason == null) {
                    exceptionReason = "unknown reason";
                }
                OpenstudInvalidResponseException invalidResponse = new OpenstudInvalidResponseException("invalid HTML: " + exceptionReason).setHTMLType();

                if (allFailedForSameReason && firstEncounteredException != null) {
                    invalidResponse.initCause(firstEncounteredException);
                }

                os.log(Level.SEVERE, invalidResponse);
                throw invalidResponse;
            }

            os.log(Level.FINE, "SapienzaNewsHandler: Successfully parsed " + ret.size() + " events, " + failed + " failed.");
            return ret;

        } catch (IOException e) {
            OpenstudConnectionException connectionException = new OpenstudConnectionException(e);
            os.log(Level.SEVERE, connectionException);
            throw connectionException;
        }
    }
}