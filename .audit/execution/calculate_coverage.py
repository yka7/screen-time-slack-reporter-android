import xml.etree.ElementTree as ET
import sys
import os

try:
    tree = ET.parse('app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml')
    root = tree.getroot()
    
    counters = root.findall('counter')
    
    metrics = {}
    for counter in counters:
        type_ = counter.get('type')
        covered = int(counter.get('covered'))
        missed = int(counter.get('missed'))
        total = covered + missed
        if total > 0:
            metrics[type_] = (covered / total) * 100
        else:
            metrics[type_] = 0.0
            
    print(f"INSTRUCTION: {metrics.get('INSTRUCTION', 0):.2f}%")
    print(f"BRANCH: {metrics.get('BRANCH', 0):.2f}%")
    print(f"LINE: {metrics.get('LINE', 0):.2f}%")
    print(f"COMPLEXITY: {metrics.get('COMPLEXITY', 0):.2f}%")
    print(f"METHOD: {metrics.get('METHOD', 0):.2f}%")
    print(f"CLASS: {metrics.get('CLASS', 0):.2f}%")

except Exception as e:
    print(f"Error: {e}")
    sys.exit(1)
