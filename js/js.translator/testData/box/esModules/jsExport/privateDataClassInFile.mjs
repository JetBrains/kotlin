import * as tableDriver from "./privateDataClassInFile_v5.mjs"

export default function box() {
    return {
        value: tableDriver.foo(),
        private: tableDriver.PrivateTable,
        public: tableDriver.PublicTable
    };
};