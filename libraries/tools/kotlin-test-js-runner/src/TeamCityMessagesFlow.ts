import {dateTimeWithoutTimeZone, newFlowId} from "./utils"
import {tcEscape} from "./teamcity-format";

export type TeamCityMessageData = { [key: string]: any }

export class TeamCityMessagesFlow {
    public readonly id: number;

    constructor(id: number | null, private readonly send: (payload: string) => void) {
        this.id = id || newFlowId()
    }

    sendMessage(type: string, args: TeamCityMessageData) {
        args['flowId'] = this.id;
        args['timestamp'] = dateTimeWithoutTimeZone();

        const serializedArgs = Object
            .keys(args)
            .map((key) => `${key}='${tcEscape(args[key])}'`)
            .join(' ');

        this.send(`##teamcity[${type} ${serializedArgs}]`)
    }
}