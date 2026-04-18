import os
from html.parser import HTMLParser

class ReportParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.in_failure = False
        self.current_test = None
        self.current_content = []
        self.results = {}
        self.filtered_count = 0

    def handle_starttag(self, tag, attrs):
        if tag == 'pre':
            attrs_dict = dict(attrs)
            id_val = attrs_dict.get('id', '')
            if id_val.startswith('root-0-test-failure-'):
                self.in_failure = True
                self.current_test = id_val[len('root-0-test-failure-'):]
                self.current_content = []

    def handle_data(self, data):
        if self.in_failure:
            self.current_content.append(data)

    def handle_endtag(self, tag):
        if tag == 'pre' and self.in_failure:
            self.in_failure = False
            content = "".join(self.current_content)
            if "TestAbortedException" not in content:
                self.results[self.current_test] = content
            else:
                self.filtered_count += 1
                # print(f"Filtered {self.current_test} (Aborted)")

def process_reports():
    report_dir = "compiler/jklib.tests/build/reports/tests/test/"
    out_file = "jklib_test_errors.md"
    
    results = {}
    total_filtered = 0
    for root, dirs, files in os.walk(report_dir):
        for f in files:
            if f.endswith('.html'):
                path = os.path.join(root, f)
                try:
                    with open(path, 'r', encoding='utf-8', errors='ignore') as file:
                        parser = ReportParser()
                        parser.feed(file.read())
                        results.update(parser.results)
                        total_filtered += parser.filtered_count
                except Exception as e:
                    print(f"Error processing {path}: {e}")
                    
    with open(out_file, 'w', encoding='utf-8') as f:
        for test, content in results.items():
            f.write(f"# {test}\n")
            f.write(content.strip())
            f.write("\n\n")
            
    print(f"Updated {out_file} with {len(results)} failures.")
    print(f"Filtered out {total_filtered} aborted tests.")

if __name__ == "__main__":
    process_reports()
