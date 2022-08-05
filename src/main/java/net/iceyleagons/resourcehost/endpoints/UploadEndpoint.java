package net.iceyleagons.resourcehost.endpoints;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.Extensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.iceyleagons.resourcehost.endpoints.exceptions.*;
import net.iceyleagons.resourcehost.security.BlacklistService;
import net.iceyleagons.resourcehost.RepoManager;
import net.iceyleagons.resourcehost.security.RateLimitService;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author TOTHTOMI
 * @version 1.0.0
 * @since Nov. 23, 2021
 */
@RestController
@RequiredArgsConstructor(onConstructor__ = @Autowired)
public class UploadEndpoint {

    private final BlacklistService blacklistService;
    private final RepoManager repoManager;
    private final RateLimitService rateLimitService;

    @CrossOrigin
    @PostMapping(value = "/api/upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> handleFileUpload(
            @Parameter(required = true, name = "file", description = "Resourcepack File")
            @RequestPart("file") @RequestParam("file") MultipartFile file, HttpServletRequest request) throws InternalErrorException, RateLimitedException, IpBlacklistedException, BadRequestException, UnsupportedMediaTypeException, PayloadTooLargeException {
        JSONObject jsonObject = new JSONObject();

        try {
            if (blacklistService.isBlacklisted(request.getRemoteAddr())) throw new IpBlacklistedException("IP is banned due to excessive requests or suspicious behavior! If you think it's not reasonable, please contact the owner of this ResourceHost instance. IP: " + request.getRemoteAddr());
            if (file.isEmpty()) throw new BadRequestException("Empty file!");
            if (file.getSize() >= repoManager.getMbLimit()) throw new PayloadTooLargeException("Upload limit exceeded! Please do not go over " + repoManager.getMbRaw() + " MBs!");

            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            if (extension == null) {
                jsonObject.put("error", "Could not read file extension!");
                return new ResponseEntity<>(jsonObject.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if (!extension.equalsIgnoreCase("zip")) {
                throw new UnsupportedMediaTypeException("Unsupported media type! Please, only upload a file with a .zip extension!");
            }

            handleRateLimit(request.getRemoteAddr());
            if (!repoManager.upload(jsonObject, file)) throw new InternalErrorException();

            return new ResponseEntity<>(jsonObject.toString(), HttpStatus.OK);
        } catch (RateLimitedException | IpBlacklistedException | BadRequestException | InternalErrorException | UnsupportedMediaTypeException | PayloadTooLargeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalErrorException();
        }
    }

    private void handleRateLimit(String ip) throws ExecutionException {
        Bucket bucket = rateLimitService.getUpload(ip);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(rateLimitService.getDownloadCost());

        if (probe.isConsumed()) {
            return;
        }
        blacklistService.rateLimitExceed(ip);
        throw new RateLimitedException("Too many requests!", TimeUnit.NANOSECONDS.toMillis(probe.getNanosToWaitForRefill()));
    }
}
