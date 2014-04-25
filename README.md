# Cloudify Custom Events

## Description

Provides a REST API to handle lifecycle evens for Cloudify 2.x.

It uses the management space to store the events.

## Deployment

1. Retrieve the zip archive from the assembly module then unzip the context into the upload folder of your prefered cloud driver (i.e. gigaspaces-cloudify-2.7.0-ga/clouds/openstack-havana/upload)

2. Then edit the bootstrap-management.sh script to add this:

```
	if [ "$GSA_MODE" = "lus" ]; then
		${WORKING_HOME_DIRECTORY}/events/bin/gsDeployEventsWar.sh
	fi
```

    Just after:
    
```
	./cloudify.sh $START_COMMAND $START_COMMAND_ARGS
```

3. Once deployed, a http GET request on `http://management_ip:8081/events/test` should returns a  `is running` message.