package alien4cloud.paas.cloudify2.events.polling.handler;

import java.util.Map;

import org.apache.commons.collections.MapUtils;

public final class RestUtils {

    private static final String INVOCATION_INSTANCE_ID_KEY = "Invocation_Instance_ID";
    private static final String INVOCATION_RESULT_KEY = "Invocation_Result";
    private static final String INVOCATION_SUCCESS_KEY = "Invocation_Success";
    private static final String INVOCATION_SUCCESS = "SUCCESS";
    private static final String INVOCATION_EXCEPTION_KEY = "Invocation_Exception";

    /**
     * Parse the response of a custom command invocation on a service level
     *
     * @param success
     * @param invocationResultPerInstance
     * @param failures TODO
     */
    public static void parseServiceInvokeResponse(Map<String, String> success, Map<String, String> failures,
            Map<String, Map<String, String>> invocationResultPerInstance) {
        if (MapUtils.isEmpty(invocationResultPerInstance)) {
            return;
        }
        for (Map<String, String> invocationResult : invocationResultPerInstance.values()) {
            parseInstanceInvokeResponse(success, failures, invocationResult);
        }
    }

    /**
     * Parse the response of a custom command invocation on an instance level
     *
     * @param successes Map containing succeeded responses
     * @param failures Map containing failed responses
     * @param invocationResult
     */
    public static void parseInstanceInvokeResponse(Map<String, String> successes, Map<String, String> failures, Map<String, String> invocationResult) {
        if (MapUtils.isEmpty(invocationResult)) {
            return;
        }
        if (INVOCATION_SUCCESS.equals(invocationResult.get(INVOCATION_SUCCESS_KEY))) {
            successes.put(invocationResult.get(INVOCATION_INSTANCE_ID_KEY), invocationResult.get(INVOCATION_RESULT_KEY));
        } else {
            failures.put(invocationResult.get(INVOCATION_INSTANCE_ID_KEY), invocationResult.get(INVOCATION_EXCEPTION_KEY));
        }

        // // if not success, throw an exception
        // throw new OperationExecutionException("InstanceId: " + invocationResult.get(INVOCATION_INSTANCE_ID_KEY) + "\n\t"
        // + invocationResult.get(INVOCATION_EXCEPTION_KEY));
    }

}
