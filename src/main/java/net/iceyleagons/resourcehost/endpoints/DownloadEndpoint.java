package net.iceyleagons.resourcehost.endpoints;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import net.iceyleagons.resourcehost.RepoManager;
import net.iceyleagons.resourcehost.endpoints.exceptions.BadRequestException;
import net.iceyleagons.resourcehost.endpoints.exceptions.InternalErrorException;
import net.iceyleagons.resourcehost.endpoints.exceptions.IpBlacklistedException;
import net.iceyleagons.resourcehost.endpoints.exceptions.RateLimitedException;
import net.iceyleagons.resourcehost.responses.ErrorResponse;
import net.iceyleagons.resourcehost.responses.ExistsResponse;
import net.iceyleagons.resourcehost.responses.RateLimitResponse;
import net.iceyleagons.resourcehost.security.BlacklistService;
import net.iceyleagons.resourcehost.security.RateLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Nov. 28, 2021
 */
@RestController
@RequiredArgsConstructor(onConstructor__ = @Autowired)
public class DownloadEndpoint {

    private final BlacklistService blacklistService;
    private final RepoManager repoManager;
    private final RateLimitService rateLimitService;

    @CrossOrigin
    @Operation(summary = "Check if a resourcepack exists with the specified id.")
    @ApiResponse(responseCode = "200", description = "Resourcepack found", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ExistsResponse.class))
    })
    @ApiResponse(responseCode = "400", description = "Bad Request", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
    })
    @ApiResponse(responseCode = "403", description = "Blacklisted IP", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
    })
    @ApiResponse(responseCode = "429", description = "Too Many Requests (rate-limited)", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = RateLimitResponse.class))
    })
    @ApiResponse(responseCode = "500", description = "Internal Error (unseen exceptions)", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
    })
    @GetMapping(value = "/api/exists/{id:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExistsResponse> exists(@Parameter(description = "Id of the resourcepack to be checked") @PathVariable String id, HttpServletRequest request) throws InternalErrorException, RateLimitedException, IpBlacklistedException, BadRequestException {
        try {
            if (blacklistService.isBlacklisted(request.getRemoteAddr())) {
                throw new IpBlacklistedException("IP is banned due to excessive requests or suspicious behavior! If you think it's not reasonable, please contact the owner of this ResourceHost instance. IP: " + request.getRemoteAddr());
            }

            if (id == null || id.isEmpty()) {
                throw new BadRequestException("No ID given or it's empty!");
            }

            long remainingTokens = handleRateLimitOther(request.getRemoteAddr());

            if (repoManager.contains(id)) {
                return new ResponseEntity<>(ExistsResponse.found(repoManager.getDownloadUrl(id), repoManager.getAvailability(id), remainingTokens), HttpStatus.OK);
            }

            return new ResponseEntity<>(ExistsResponse.empty(remainingTokens), HttpStatus.OK);
        } catch (RateLimitedException | IpBlacklistedException | BadRequestException | InternalErrorException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalErrorException();
        }
    }

    @ResponseBody
    @GetMapping( value = "/get/{id:.+}", produces = MediaType.MULTIPART_MIXED_VALUE)
    public ResponseEntity<byte[]> serveFile(@PathVariable String id, HttpServletRequest request) throws InternalErrorException, RateLimitedException, IpBlacklistedException, BadRequestException {
        try {
            handleRateLimitDownload(request.getRemoteAddr());

            byte[] data = repoManager.readFile(id);
            if (data == null) return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + id + ".zip\"").body(data);
        } catch (RateLimitedException | IpBlacklistedException | BadRequestException | InternalErrorException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalErrorException();
        }
    }

    private long handleRateLimitOther(String ip) throws ExecutionException {
        Bucket bucket = rateLimitService.getGeneral(ip);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return probe.getRemainingTokens();
        }

        blacklistService.rateLimitExceed(ip);
        throw new RateLimitedException("Too many requests!", TimeUnit.NANOSECONDS.toMillis(probe.getNanosToWaitForRefill()));
    }

    private void handleRateLimitDownload(String ip) throws ExecutionException {
        Bucket bucket = rateLimitService.getDownload(ip);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(rateLimitService.getDownloadCost());

        if (probe.isConsumed()) {
            return;
        }
        blacklistService.rateLimitExceed(ip);
        throw new RateLimitedException("Too many requests!", TimeUnit.NANOSECONDS.toMillis(probe.getNanosToWaitForRefill()));
    }
}
