package part1;

/*
skierID - between 1 and 100000
resortID - between 1 and 10
liftID - between 1 and 40
seasonID - 2022
dayID - 1
time - between 1 and 360
 */

import static bean.defaultConstants.DEFAULT_POST_TYPE;

import part2.ResponseRecord;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import io.swagger.client.model.SwipeDetails;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class SkierThread extends Thread{
  private int skierStartId;
  private int skierEndId;
  private int resortRange;
  private String seasonId;
  private String dayId;
  private int startTime;
  private int endTime;
  private int numPost;
  private int liftNum;
  private int request;
  private int success;
  private int failure;
  private CountDownLatch threadLatch;
  private CountDownLatch totalLatch;
  private BlockingQueue<ResponseRecord> responseRecords;
  private String baseUrl;

  public SkierThread(int skierStartId, int skierEndId, int resortId, String seasonId, String dayId,
      int startTime, int endTime, int numPost, int liftNumId,
      CountDownLatch threadLatch, CountDownLatch totalLatch, BlockingQueue<ResponseRecord> ResponseRecords,
      String baseUrl) {
    this.skierStartId = skierStartId;
    this.skierEndId = skierEndId;
    this.resortRange = resortId;
    this.seasonId = seasonId;
    this.dayId = dayId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.numPost = numPost;
    this.liftNum = liftNumId;
    this.request = 0;
    this.success = 0;
    this.failure = 0;
    this.threadLatch = threadLatch;
    this.totalLatch = totalLatch;
    this.responseRecords = ResponseRecords;
    this.baseUrl = baseUrl;
  }

  @Override
  public void run() {
    try{
      // send post here
      while(this.request < 1000){
        SwipeApi skiersApi = new SwipeApi();
        ApiClient apiClient = skiersApi.getApiClient();
        apiClient.setBasePath(this.baseUrl);
        boolean isFailed = false;

        ResponseRecord currResponse = sendPost(skiersApi);
        // success
        if(currResponse.getResponseStatus() >= 200 && currResponse.getResponseStatus() < 300){
          this.success ++;
          this.responseRecords.add(currResponse);
          // System.out.println("thread success with code:" + currResponse.getResponseStatus());
        }
        // retry
        else if(currResponse.getResponseStatus() >= 400 && currResponse.getResponseStatus() < 600){
          isFailed = true;
          // handle errors, retry 5 times
          for(int j = 0; j < 5; j++){
            currResponse = sendPost(skiersApi);
            if(currResponse.getResponseStatus() >= 200 && currResponse.getResponseStatus() < 300){
              isFailed = false;
              break;
            }
          }
        }

        if(isFailed){
          // add failed record into records
          this.failure ++;
          this.responseRecords.add(currResponse);
          System.out.println("thread failed with code:" + currResponse.getResponseStatus());
        }
        if(totalLatch != null){
          totalLatch.countDown();
          System.out.println(totalLatch);
        }
      }
      } catch (ApiException e) {
        this.failure ++;
        throw new RuntimeException(e);
      }
  }

  public int getRandomRange(int min, int max){
    return ThreadLocalRandom.current().nextInt(min, max);
  }

  private ResponseRecord sendPost(SwipeApi skiersApi) throws ApiException {
    String leftorright = "";
    int left = getRandomRange(0,2);
    if (left == 0){
      leftorright = "left";
    }
    else {
      leftorright = "right";
    }
    Integer swiper = getRandomRange(0, 5000+1);
    Integer swipee = getRandomRange(1, 1000000 + 1);
    Integer comment = getRandomRange(1,27);
    SwipeDetails detail = new SwipeDetails();
    // start a new liftride
    detail.setSwiper(swiper.toString());
    detail.setSwipee(swipee.toString());
    detail.setComment(comment.toString());
    long postStartTime = System.currentTimeMillis();
    ApiResponse<Void> apiResponse = skiersApi.swipeWithHttpInfo(detail,leftorright );
    long postEndTime = System.currentTimeMillis();
    this.request ++;
    return new ResponseRecord(postStartTime, postEndTime - postStartTime, apiResponse.getStatusCode(), DEFAULT_POST_TYPE);
  }

  public int getRequest() {
    return request;
  }

  public int getSuccess() {
    return success;
  }

  public int getFailure() {
    return failure;
  }
}
